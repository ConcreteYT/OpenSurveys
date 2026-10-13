package com.opensurveys.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import com.opensurveys.security.ClientRateLimiter;
import com.opensurveys.service.AccessCodes;
import com.opensurveys.service.CurrentUserService;
import com.opensurveys.service.FormAccessService;
import com.opensurveys.service.RespondentTokenService;
import com.opensurveys.service.UploadStorageService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.opensurveys.dto.AnswerRequest;
import com.opensurveys.dto.AnswerResponse;
import com.opensurveys.dto.AnswerSubmissionRequest;
import com.opensurveys.dto.FormRequest;
import com.opensurveys.dto.FormResponse;
import com.opensurveys.dto.FormResultsResponse;
import com.opensurveys.dto.QuestionRequest;
import com.opensurveys.dto.QuestionResultsResponse;
import com.opensurveys.model.Answer;
import com.opensurveys.model.Form;
import com.opensurveys.model.Question;
import com.opensurveys.model.QuestionType;
import com.opensurveys.model.User;
import com.opensurveys.repository.AnswerRepository;
import com.opensurveys.repository.FormRepository;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Owns the form-facing routes that together implement "sign in/register only to
// create, list, or edit your forms, but anyone can fill one out":
//  - POST /forms, GET /forms, PUT /forms/{id} are locked down in SecurityConfig (authenticated()).
//  - GET /forms/{id} is public (permitAll() in SecurityConfig) - what an anonymous filler's
//    browser calls, and it surfaces the creator's username for display.
@RestController
public class FormController {

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private FormAccessService formAccessService;

    @Autowired
    private UploadStorageService uploadStorageService;

    @Autowired
    private RespondentTokenService respondentTokenService;

    @Autowired
    private ClientRateLimiter rateLimiter;

    private final SecureRandom secureRandom = new SecureRandom();

    // Requires auth. Returns only forms created by the JWT principal (newest first).
    @GetMapping("/forms")
    public ResponseEntity<?> listMyForms() {
        Optional<User> userOptional = currentUserService.get();
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<FormResponse> responses = formRepository.findByCreatorOrderByIdDesc(userOptional.get()).stream()
                .map(FormResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // Requires auth. SecurityConfig already blocks anonymous requests before they reach
    // here, but this is a defensive second check in case that config ever changes.
    // The creator is derived from the JWT (via JwtAuthFilter -> SecurityContextHolder),
    // never from the request body, so a caller can't fabricate a form under someone else's name.
    @PostMapping("/forms")
    public ResponseEntity<?> createForm(@RequestBody FormRequest formRequest) {
        Optional<User> userOptional = currentUserService.get();
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User creator = userOptional.get();

        List<QuestionRequest> questionRequests = questionRequestsOf(formRequest);
        Optional<ResponseEntity<?>> validationError = validateQuestionRequests(questionRequests);
        if (validationError.isPresent()) {
            return validationError.get();
        }

        Form form = new Form();
        applyFormFields(form, formRequest);
        form.setCreator(creator);

        // Build one Question entity per QuestionRequest and point each back at this Form
        // (Question owns the FK, FORM_form_id) so Form's cascade=ALL persists them together
        // in a single formRepository.save() call below.
        for (QuestionRequest questionRequest : questionRequests) {
            form.getQuestions().add(newQuestion(form, questionRequest));
        }

        Form savedForm = saveNewForm(form);

        return ResponseEntity.status(HttpStatus.CREATED).body(FormResponse.from(savedForm));
    }

    // Requires auth. Only the form's creator may update. Questions with an id that already
    // belong to this form are updated in place (existing answers stay linked). Questions
    // without an id are created. Questions omitted from the request are removed (and their
    // answers cascade-deleted via Question.answers orphanRemoval).
    @PutMapping("/forms/{id}")
    public ResponseEntity<?> updateForm(@PathVariable Long id, @RequestBody FormRequest formRequest) {
        Optional<User> userOptional = currentUserService.get();
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userOptional.get();

        Optional<Form> formOptional = formRepository.findById(id);
        if (formOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Form form = formOptional.get();

        if (!formAccessService.canManageForm(form, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only edit surveys you created"));
        }

        List<QuestionRequest> questionRequests = questionRequestsOf(formRequest);
        Optional<ResponseEntity<?>> validationError = validateQuestionRequests(questionRequests);
        if (validationError.isPresent()) {
            return validationError.get();
        }

        Map<Long, Question> existingById = form.questionsById();
        Set<Long> keepIds = new HashSet<>();
        List<Question> additions = new ArrayList<>();

        for (QuestionRequest questionRequest : questionRequests) {
            Long questionId = questionRequest.getId();
            if (questionId != null) {
                Question existing = existingById.get(questionId);
                if (existing == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error",
                                    "Question id " + questionId + " does not belong to this survey"));
                }
                applyQuestionFields(existing, questionRequest);
                keepIds.add(questionId);
            } else {
                additions.add(newQuestion(form, questionRequest));
            }
        }

        // orphanRemoval deletes removed questions (and cascaded answers). Kept questions
        // stay managed so prior responses remain attached.
        form.getQuestions().removeIf(question ->
                question.getId() != null && !keepIds.contains(question.getId()));
        form.getQuestions().addAll(additions);

        applyFormFields(form, formRequest);
        Form savedForm = formRepository.save(form);
        return ResponseEntity.ok(FormResponse.from(savedForm));
    }

    // Public/anonymous - no token required (see SecurityConfig permitAll for GET /forms/**).
    // This is the endpoint a form-filling page calls; the returned creatorUsername is what
    // lets the frontend show "Created by <username>" at the top of the fill-out view.
    // Public lookup by the 8-digit access code. The numeric id in the response is what
    // /access?formId= and the editor keep using. /forms/{id} cannot capture this path:
    // that variable is a Long, and this mapping has an extra segment.
    @GetMapping("/forms/by-code/{accessCode}")
    public ResponseEntity<?> getFormByAccessCode(@PathVariable String accessCode,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        if (!rateLimiter.allow("form-code:" + request.getRemoteAddr(), 30, 60_000L)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "too many requests"));
        }
        if (!AccessCodes.isValid(accessCode)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Access code must be exactly 8 digits"));
        }
        return formRepository.findByAccessCode(accessCode)
                .map(form -> ResponseEntity.ok(respondentView(form, request, response)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/forms/{id}")
    public ResponseEntity<?> getForm(@PathVariable Long id,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        Optional<User> userOptional = currentUserService.get();
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Form> formOptional = formRepository.findById(id);
        if (formOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Form form = formOptional.get();
        if (!formAccessService.canManageForm(form, userOptional.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only view surveys you created"));
        }
        boolean alreadySubmitted = respondentTokenService.prepare(
                form.getId(), form.isOneResponseOnly(), request, response);
        FormResponse body = FormResponse.from(form);
        body.setAlreadySubmitted(alreadySubmitted);
        return ResponseEntity.ok(body);
    }

    // Aggregated answers for a form. Public when form.responsesPublic is true (anyone with
    // the id). Private forms require the authenticated creator — JWT is optional on this
    // route (permitAll) so JwtAuthFilter can still populate the principal when a token is sent.
    @GetMapping("/forms/{id}/responses")
    public ResponseEntity<?> getFormResponses(@PathVariable Long id) {
        Optional<Form> formOptional = formRepository.findById(id);
        if (formOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Form form = formOptional.get();
        Optional<User> userOptional = currentUserService.get();
        if (!formAccessService.canViewResponses(form, userOptional)) {
            if (formAccessService.adminBlockedFromResponses(form, userOptional)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Administrators cannot view survey responses"));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Responses for this survey are private"));
        }

        Map<Long, List<String>> answersByQuestionId = new HashMap<>();
        for (AnswerRepository.QuestionAnswer saved : answerRepository.findQuestionAnswersByFormId(id)) {
            answersByQuestionId
                    .computeIfAbsent(saved.getQuestionId(), key -> new ArrayList<>())
                    .add(saved.getAnswer());
        }

        List<QuestionResultsResponse> questionResults = new ArrayList<>();
        for (Question question : form.getQuestions()) {
            questionResults.add(new QuestionResultsResponse(
                    question.getId(),
                    question.getQuestionText(),
                    question.getQuestionType(),
                    question.getQuestionOptions(),
                    answersByQuestionId.getOrDefault(question.getId(), List.of())
            ));
        }

        return ResponseEntity.ok(new FormResultsResponse(
                form.getId(),
                form.getName(),
                questionResults
        ));
    }

    // Public/anonymous - same reasoning as getForm above: whoever filled out the form
    // (via GET /forms/{id}) never had to log in, so submitting their answers can't require
    // a token either. See SecurityConfig for the matching permitAll() rule.
    //
    // Client flow this closes the loop on: GET /forms/{id} sends the questions (each with
    // its own id via QuestionResponse.id) -> client shows them and collects answers in the
    // background -> client POSTs this endpoint with one AnswerRequest per question, reusing
    // those same question ids -> we persist one ANSWER row per AnswerRequest, each linked to
    // its Question via the FK (see Answer.question).
    @PostMapping("/forms/{id}/answers")
    @Transactional
    public ResponseEntity<?> submitAnswers(@PathVariable Long id,
                                           @RequestBody AnswerSubmissionRequest submissionRequest,
                                           HttpServletRequest request) {
        Optional<Form> formOptional = formRepository.findById(id);
        if (formOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Form form = formOptional.get();
        if (form.hasImageUploadQuestions()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Use POST /forms/{id}/staging/{stagingId}/commit for this survey"));
        }

        if (submissionRequest.getAnswers() == null || submissionRequest.getAnswers().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Resolve every AnswerRequest into one of this form's Questions first (and validate it)
        // before saving anything, so a single bad questionId - unknown, or belonging to a
        // different form than the one in the URL - rejects the whole submission instead of
        // partially saving answers.
        Map<Long, Question> questionsById = form.questionsById();
        List<Answer> answersToSave = new ArrayList<>();
        for (AnswerRequest answerRequest : submissionRequest.getAnswers()) {
            if (answerRequest.getQuestionId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            Question question = questionsById.get(answerRequest.getQuestionId());
            if (question == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            Answer answer = new Answer();
            answer.setAnswer(answerRequest.getAnswer());
            answer.setQuestion(question);
            answersToSave.add(answer);
        }

        if (form.isOneResponseOnly() && !respondentTokenService.consume(form.getId(), request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "This survey already has a response from this browser"));
        }

        List<Answer> savedAnswers = answerRepository.saveAll(answersToSave);
        return ResponseEntity.status(HttpStatus.CREATED).body(AnswerResponse.fromAll(savedAnswers));
    }

    @DeleteMapping("/forms/{id}")
    public ResponseEntity<?> deleteForm(@PathVariable Long id) {
        Optional<User> userOptional = currentUserService.get();
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userOptional.get();

        Optional<Form> formOptional = formRepository.findById(id);
        if (formOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Form form = formOptional.get();
        if (!formAccessService.canManageForm(form, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only delete surveys you created"));
        }

        try {
            uploadStorageService.deleteFormUploads(id);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete uploaded files"));
        }
        formRepository.delete(form);
        return ResponseEntity.noContent().build();
    }

    // Retries when the generated code is already taken or the unique constraint rejects the insert.
    private Form saveNewForm(Form form) {
        for (int attempt = 0; attempt < AccessCodes.MAX_ATTEMPTS; attempt++) {
            String code = AccessCodes.generate(secureRandom);
            if (formRepository.findByAccessCode(code).isPresent()) {
                continue;
            }
            form.setAccessCode(code);
            try {
                return formRepository.save(form);
            } catch (DataIntegrityViolationException ex) {
                form.setAccessCode(null);
                if (form.getId() != null) {
                    form.setId(null);
                    for (Question question : form.getQuestions()) {
                        question.setId(null);
                    }
                }
                if (attempt == AccessCodes.MAX_ATTEMPTS - 1) {
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Could not allocate a unique access code");
    }

    // Returns empty when valid; otherwise a ready-to-return error ResponseEntity.
    private Optional<ResponseEntity<?>> validateQuestionRequests(List<QuestionRequest> questionRequests) {
        for (QuestionRequest questionRequest : questionRequests) {
            if (!QuestionType.isValid(questionRequest.getQuestionType())) {
                return Optional.of(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "questionType must be 0 (SKIPPABLE_TEXT), 1 (TEXT), 2 (MULTIPLE_CHOICE), 3 (RATING), or 4 (IMAGE_UPLOAD)")));
            }

            Integer type = questionRequest.getQuestionType();
            String options = questionRequest.getQuestionOptions();
            if (type == QuestionType.IMAGE_UPLOAD
                    && QuestionType.parseImageUploadMaxFiles(options) == null) {
                return Optional.of(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "IMAGE_UPLOAD questions require questionOptions as max files from 1 to 10 (e.g. \"3\")")));
            }
            if (type == QuestionType.MULTIPLE_CHOICE
                    && QuestionType.parseMultipleChoice(options) == null) {
                return Optional.of(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "MULTIPLE_CHOICE questions require semicolon-separated options, "
                                        + "optionally ending with N! for required selections "
                                        + "(e.g. \"Math;Physics;Informatik\" or \"Math;Physics;Informatik;2!\")")));
            }
            if (type == QuestionType.RATING
                    && QuestionType.parseRatingMax(options) == null) {
                return Optional.of(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "RATING questions require questionOptions as a whole number from 5 to 10 (e.g. \"5\" or \"10\")")));
            }
        }
        return Optional.empty();
    }

    private static List<QuestionRequest> questionRequestsOf(FormRequest formRequest) {
        return formRequest.getQuestions() != null ? formRequest.getQuestions() : List.of();
    }

    private FormResponse respondentView(Form form, HttpServletRequest request, HttpServletResponse response) {
        boolean alreadySubmitted = respondentTokenService.prepare(
                form.getId(), form.isOneResponseOnly(), request, response);
        return FormResponse.forRespondent(form, alreadySubmitted);
    }

    // responsesPublic defaults to private. oneResponseOnly defaults to true when omitted.
    private static void applyFormFields(Form form, FormRequest formRequest) {
        form.setName(formRequest.getName());
        form.setResponsesPublic(Boolean.TRUE.equals(formRequest.getResponsesPublic()));
        form.setOneResponseOnly(formRequest.getOneResponseOnly() == null || formRequest.getOneResponseOnly());
    }

    private static Question newQuestion(Form form, QuestionRequest questionRequest) {
        Question question = new Question();
        applyQuestionFields(question, questionRequest);
        question.setForm(form);
        return question;
    }

    private static void applyQuestionFields(Question question, QuestionRequest questionRequest) {
        question.setQuestionText(questionRequest.getQuestionText());
        question.setQuestionType(questionRequest.getQuestionType());
        question.setQuestionOptions(questionRequest.getQuestionOptions());
    }
}
