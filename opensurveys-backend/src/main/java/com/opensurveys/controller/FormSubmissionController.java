package com.opensurveys.controller;

import com.opensurveys.dto.AnswerRequest;
import com.opensurveys.dto.AnswerResponse;
import com.opensurveys.dto.AnswerSubmissionRequest;
import com.opensurveys.model.Answer;
import com.opensurveys.model.Form;
import com.opensurveys.model.Question;
import com.opensurveys.model.QuestionType;
import com.opensurveys.repository.AnswerRepository;
import com.opensurveys.repository.FormRepository;
import com.opensurveys.repository.QuestionRepository;
import com.opensurveys.service.UploadStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
public class FormSubmissionController {

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private UploadStorageService uploadStorageService;

    @PostMapping(
            value = "/forms/{formId}/staging/{stagingId}/questions/{questionId}/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> stageQuestionFiles(
            @PathVariable Long formId,
            @PathVariable String stagingId,
            @PathVariable Long questionId,
            @RequestParam("files") List<MultipartFile> files
    ) {
        try {
            UploadStorageService.validateStagingId(stagingId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid staging id"));
        }

        Optional<Form> formOptional = formRepository.findById(formId);
        if (formOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<Question> questionOptional = questionRepository.findById(questionId);
        if (questionOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Unknown question"));
        }
        Question question = questionOptional.get();
        if (question.getForm() == null || !question.getForm().getId().equals(formId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Question does not belong to this survey"));
        }
        if (question.getQuestionType() == null || question.getQuestionType() != QuestionType.IMAGE_UPLOAD) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Question is not an image upload type"));
        }

        Integer maxFiles = QuestionType.parseImageUploadMaxFiles(question.getQuestionOptions());
        if (maxFiles == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid image upload configuration"));
        }

        try {
            uploadStorageService.ensureRootExists();
            List<String> stored = uploadStorageService.stageQuestionFiles(formId, stagingId, questionId, maxFiles, files);
            return ResponseEntity.ok(Map.of("fileNames", stored));
        } catch (UploadStorageService.UploadValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to store files"));
        }
    }

    @PostMapping("/forms/{formId}/staging/{stagingId}/commit")
    public ResponseEntity<?> commitSubmission(
            @PathVariable Long formId,
            @PathVariable String stagingId,
            @RequestBody AnswerSubmissionRequest submissionRequest
    ) {
        try {
            UploadStorageService.validateStagingId(stagingId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid staging id"));
        }

        Optional<Form> formOptional = formRepository.findById(formId);
        if (formOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Form form = formOptional.get();

        if (submissionRequest.getAnswers() == null) {
            submissionRequest.setAnswers(List.of());
        }

        Map<Long, Question> questionsById = new HashMap<>();
        for (Question question : form.getQuestions()) {
            if (question.getId() != null) {
                questionsById.put(question.getId(), question);
            }
        }

        boolean hasUploadQuestions = form.getQuestions().stream()
                .anyMatch(q -> q.getQuestionType() != null && q.getQuestionType() == QuestionType.IMAGE_UPLOAD);

        if (!hasUploadQuestions) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "This survey has no image upload questions; use POST /forms/{id}/answers"));
        }

        Set<Long> textAnswerQuestionIds = new HashSet<>();
        List<Answer> textAnswersToSave = new ArrayList<>();

        for (AnswerRequest answerRequest : submissionRequest.getAnswers()) {
            if (answerRequest.getQuestionId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Missing questionId"));
            }
            Question question = questionsById.get(answerRequest.getQuestionId());
            if (question == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid questionId"));
            }
            Integer type = question.getQuestionType();
            if (type != null && type == QuestionType.IMAGE_UPLOAD) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Image upload answers must be submitted via staging, not in JSON"));
            }
            if (type != null && type == QuestionType.SKIPPABLE_TEXT) {
                continue;
            }
            if (answerRequest.getAnswer() == null || answerRequest.getAnswer().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Missing answer"));
            }
            textAnswerQuestionIds.add(question.getId());
            Answer answer = new Answer();
            answer.setQuestion(question);
            answer.setAnswer(answerRequest.getAnswer().trim());
            textAnswersToSave.add(answer);
        }

        for (Question question : form.getQuestions()) {
            Integer type = question.getQuestionType();
            if (type == null || type == QuestionType.SKIPPABLE_TEXT || type == QuestionType.IMAGE_UPLOAD) {
                continue;
            }
            if (!textAnswerQuestionIds.contains(question.getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Missing answer for question " + question.getId()));
            }
        }

        List<Answer> uploadAnswersToSave = new ArrayList<>();
        List<String> movedFilesForRollback = new ArrayList<>();

        try {
            uploadStorageService.ensureRootExists();

            for (Question question : form.getQuestions()) {
                if (question.getQuestionType() == null || question.getQuestionType() != QuestionType.IMAGE_UPLOAD) {
                    continue;
                }
                Long qid = question.getId();
                List<String> staged = uploadStorageService.listStagedFileNames(formId, stagingId, qid);
                if (staged.isEmpty()) {
                    uploadStorageService.deleteStagingSession(stagingId);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Missing uploaded images for question " + qid));
                }

                String finalValue = uploadStorageService.moveStagedQuestionToFinal(formId, stagingId, qid);
                for (String part : UploadStorageService.splitAnswerFileNames(finalValue)) {
                    movedFilesForRollback.add(part);
                }

                Answer answer = new Answer();
                answer.setQuestion(question);
                answer.setAnswer(finalValue);
                uploadAnswersToSave.add(answer);
            }

            List<Answer> allToSave = new ArrayList<>(textAnswersToSave);
            allToSave.addAll(uploadAnswersToSave);

            if (allToSave.isEmpty()) {
                uploadStorageService.rollbackMovedFiles(formId, movedFilesForRollback);
                uploadStorageService.deleteStagingSession(stagingId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "No answers to save"));
            }

            List<Answer> savedAnswers = answerRepository.saveAll(allToSave);
            uploadStorageService.deleteStagingSession(stagingId);

            List<AnswerResponse> answerResponses = new ArrayList<>();
            for (Answer saved : savedAnswers) {
                answerResponses.add(new AnswerResponse(
                        saved.getId(),
                        saved.getQuestion().getId(),
                        saved.getAnswer()
                ));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(answerResponses);
        } catch (UploadStorageService.UploadValidationException e) {
            uploadStorageService.rollbackMovedFiles(formId, movedFilesForRollback);
            uploadStorageService.deleteStagingSession(stagingId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            uploadStorageService.rollbackMovedFiles(formId, movedFilesForRollback);
            uploadStorageService.deleteStagingSession(stagingId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to commit submission"));
        } catch (DataIntegrityViolationException e) {
            uploadStorageService.rollbackMovedFiles(formId, movedFilesForRollback);
            uploadStorageService.deleteStagingSession(stagingId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Could not save answers; please try again"));
        }
    }
}
