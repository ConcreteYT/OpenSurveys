package com.opensurveys.controller;

import com.opensurveys.model.Answer;
import com.opensurveys.model.Form;
import com.opensurveys.model.Question;
import com.opensurveys.model.QuestionType;
import com.opensurveys.model.User;
import com.opensurveys.repository.AnswerRepository;
import com.opensurveys.repository.FormRepository;
import com.opensurveys.repository.UserRepository;
import com.opensurveys.service.FormAccessService;
import com.opensurveys.service.UploadStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    private UserRepository userRepository;

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
        Optional<User> userOptional = resolveAuthenticatedUser();

        if (!formAccessService.canViewResponses(form, userOptional)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Not allowed to view this media"));
        }

        if (!uploadStorageService.formFileExists(formId, fileName)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (!isFileReferencedByFormAnswers(formId, fileName)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Path path = uploadStorageService.resolveFormFile(formId, fileName);
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

    @GetMapping("/forms/{formId}/export")
    public ResponseEntity<?> exportForm(@PathVariable Long formId) {
        Optional<User> userOptional = resolveAuthenticatedUser();
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userOptional.get();

        Optional<Form> formOptional = formRepository.findById(formId);
        if (formOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Form form = formOptional.get();

        if (!formAccessService.canExportResponses(form, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the survey owner can export responses"));
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
            if (question.getQuestionType() != null && question.getQuestionType() == QuestionType.IMAGE_UPLOAD) {
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

        try {
            byte[] zipBytes = buildExportZip(csv.toString(), formId, imageNames);
            String downloadName = "survey-" + formId + "-responses.zip";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .body(zipBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to build export"));
        }
    }

    private byte[] buildExportZip(String csvContent, long formId, Set<String> imageNames) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(byteStream)) {
            zip.putNextEntry(new ZipEntry("responses.csv"));
            zip.write(csvContent.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            for (String imageName : imageNames) {
                if (!uploadStorageService.formFileExists(formId, imageName)) {
                    continue;
                }
                Path path = uploadStorageService.resolveFormFile(formId, imageName);
                zip.putNextEntry(new ZipEntry("images/" + imageName));
                try (InputStream in = Files.newInputStream(path)) {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
        return byteStream.toByteArray();
    }

    private boolean isFileReferencedByFormAnswers(long formId, String fileName) {
        List<Answer> answers = answerRepository.findAllByQuestionFormIdOrderByIdAsc(formId);
        for (Answer answer : answers) {
            Question question = answer.getQuestion();
            if (question == null || question.getQuestionType() == null) {
                continue;
            }
            if (question.getQuestionType() != QuestionType.IMAGE_UPLOAD) {
                continue;
            }
            if (UploadStorageService.splitAnswerFileNames(answer.getAnswer()).contains(fileName)) {
                return true;
            }
        }
        return false;
    }

    private static String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private Optional<User> resolveAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
    }
}
