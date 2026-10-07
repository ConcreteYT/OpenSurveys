package com.opensurveys.controller;

import com.opensurveys.model.Answer;
import com.opensurveys.model.Form;
import com.opensurveys.model.Question;
import com.opensurveys.model.QuestionType;
import com.opensurveys.model.User;
import com.opensurveys.repository.AnswerRepository;
import com.opensurveys.repository.FormRepository;
import com.opensurveys.service.CurrentUserService;
import com.opensurveys.service.FormAccessService;
import com.opensurveys.service.UploadStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class FormMediaController {

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UploadStorageService uploadStorageService;

    @Autowired
    private FormAccessService formAccessService;

    @GetMapping("/forms/{formId}/media/{fileName}")
    public ResponseEntity<?> getMedia(@PathVariable Long formId, @PathVariable String fileName) {
        Optional<Form> formOptional = formRepository.findById(formId);
        if (formOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Form form = formOptional.get();
        Optional<User> userOptional = currentUserService.get();

        if (!formAccessService.canViewResponses(form, userOptional)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Not allowed to view this media"));
        }

        Optional<Path> pathOptional = uploadStorageService.findFormFile(formId, fileName);
        if (pathOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (!isFileReferencedByFormAnswers(formId, fileName)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Path path = pathOptional.get();
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            InputStreamResource resource = new InputStreamResource(Files.newInputStream(path));
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                    .body(resource);
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // The CSV and the list of image files are prepared up front; only the zip itself is streamed,
    // so image bytes never have to be held in memory. Spring only streams when the declared body
    // type is StreamingResponseBody, so early error responses go through ExportRejectedException.
    @GetMapping("/forms/{formId}/export")
    public ResponseEntity<StreamingResponseBody> exportForm(@PathVariable Long formId) {
        Optional<User> userOptional = currentUserService.get();
        if (userOptional.isEmpty()) {
            throw new ExportRejectedException(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        User user = userOptional.get();

        Optional<Form> formOptional = formRepository.findById(formId);
        if (formOptional.isEmpty()) {
            throw new ExportRejectedException(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        }
        Form form = formOptional.get();

        if (!formAccessService.canExportResponses(form, user)) {
            throw new ExportRejectedException(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the survey owner can export responses")));
        }

        List<Answer> answers = answerRepository.findAllByQuestionFormIdOrderByIdAsc(formId);
        Set<String> imageNames = new LinkedHashSet<>();

        StringBuilder csv = new StringBuilder();
        csv.append("answer_id,question_id,question_text,question_type,answer\n");
        for (Answer answer : answers) {
            Question question = answer.getQuestion();
            if (question == null) {
                continue;
            }
            String answerText = answer.getAnswer() == null ? "" : answer.getAnswer();
            if (QuestionType.is(question.getQuestionType(), QuestionType.IMAGE_UPLOAD)) {
                imageNames.addAll(UploadStorageService.splitAnswerFileNames(answerText));
            }
            csv.append(csvCell(answer.getId()))
                    .append(',')
                    .append(csvCell(question.getId()))
                    .append(',')
                    .append(csvCell(question.getQuestionText()))
                    .append(',')
                    .append(csvCell(question.getQuestionType()))
                    .append(',')
                    .append(csvCell(answerText))
                    .append('\n');
        }

        Map<String, Path> imagePaths = new LinkedHashMap<>();
        for (String imageName : imageNames) {
            uploadStorageService.findFormFile(formId, imageName)
                    .ifPresent(path -> imagePaths.put(imageName, path));
        }
        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        StreamingResponseBody body = outputStream -> writeExportZip(outputStream, csvBytes, imagePaths);
        String downloadName = "survey-" + formId + "-responses.zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(body);
    }

    @ExceptionHandler(ExportRejectedException.class)
    public ResponseEntity<?> handleExportRejected(ExportRejectedException e) {
        return e.response;
    }

    private static final class ExportRejectedException extends RuntimeException {
        private final transient ResponseEntity<?> response;

        private ExportRejectedException(ResponseEntity<?> response) {
            super(null, null, false, false);
            this.response = response;
        }
    }

    private static void writeExportZip(OutputStream outputStream, byte[] csvBytes, Map<String, Path> imagePaths)
            throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            zip.putNextEntry(new ZipEntry("responses.csv"));
            zip.write(csvBytes);
            zip.closeEntry();

            for (Map.Entry<String, Path> image : imagePaths.entrySet()) {
                zip.putNextEntry(new ZipEntry("images/" + image.getKey()));
                try (InputStream in = Files.newInputStream(image.getValue())) {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
    }

    private boolean isFileReferencedByFormAnswers(long formId, String fileName) {
        return answerRepository.findAnswersContaining(formId, QuestionType.IMAGE_UPLOAD, fileName).stream()
                .anyMatch(answer -> UploadStorageService.splitAnswerFileNames(answer).contains(fileName));
    }

    private static String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
