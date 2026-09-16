package com.opensurveys.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.opensurveys.dto.AnswerRequest;
import com.opensurveys.dto.AnswerResponse;
import com.opensurveys.dto.AnswerSubmissionRequest;
import com.opensurveys.dto.FormRequest;
import com.opensurveys.dto.FormResponse;
import com.opensurveys.dto.FormResultsResponse;
import com.opensurveys.dto.QuestionRequest;
import com.opensurveys.dto.QuestionResponse;
import com.opensurveys.dto.QuestionResultsResponse;
import com.opensurveys.model.Answer;
import com.opensurveys.model.Form;
import com.opensurveys.model.Question;
import com.opensurveys.model.QuestionType;
import com.opensurveys.model.User;
import com.opensurveys.repository.AnswerRepository;
import com.opensurveys.repository.FormRepository;
import com.opensurveys.repository.QuestionRepository;
import com.opensurveys.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private UserRepository userRepository;

    // Needed by submitAnswers below to resolve each AnswerRequest.questionId back into a
    // Question entity (previously unused, per QuestionRepository's own class comment).
    @Autowired
    private QuestionRepository questionRepository;

    // Needed by submitAnswers below to persist the new Answer rows (previously unused, per
    // AnswerRepository's own class comment).
    @Autowired
    private AnswerRepository answerRepository;

    // Requires auth. Returns only forms created by the JWT principal (newest first).
    @GetMapping("/forms")
    public ResponseEntity<?> listMyForms() {
        Optional<User> userOptional = resolveAuthenticatedUser();
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Form> forms = formRepository.findByCreatorOrderByIdDesc(userOptional.get());
        List<FormResponse> responses = new ArrayList<>();
        for (Form form : forms) {
            responses.add(toFormResponse(form));
        }
        return ResponseEntity.ok(responses);
    }

    // Requires auth. SecurityConfig already blocks anonymous requests before they reach
    // here, but this is a defensive second check in case that config ever changes.
    // The creator is derived from the JWT (via JwtAuthFilter -> SecurityContextHolder),
    // never from the request body, so a caller can't fabricate a form under someone else's name.
    @PostMapping("/forms")
    public ResponseEntity<?> createForm(@RequestBody FormRequest formRequest) {
        Optional<User> userOptional = resolveAuthenticatedUser();
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User creator = userOptional.get();

        List<QuestionRequest> questionRequests =
                formRequest.getQuestions() != null ? formRequest.getQuestions() : List.of();
        Optional<ResponseEntity<?>> validationError = validateQuestionRequests(questionRequests);
        if (validationError.isPresent()) {
            return validationError.get();
        }

        Form form = new Form();
        form.setName(formRequest.getName());
        form.setResponsesPublic(formRequest.getResponsesPublic() == null || formRequest.getResponsesPublic());
        form.setCreator(creator);

        // Build one Question entity per QuestionRequest and point each back at this Form
        // (Question owns the FK, FORM_form_id) so Form's cascade=ALL persists them together
        // in a single formRepository.save() call below.
        for (QuestionRequest questionRequest : questionRequests) {
            Question question = new Question();
            applyQuestionFields(question, questionRequest);
            question.setForm(form);
            form.getQuestions().add(question);
        }

        Form savedForm = formRepository.save(form);

        return ResponseEntity.status(HttpStatus.CREATED).body(toFormResponse(savedForm));
    }

    // Requires auth. Only the form's creator may update. Questions with an id that already
    // belong to this form are updated in place (existing answers stay linked). Questions
    // without an id are created. Questions omitted from the request are removed (and their
    // answers cascade-deleted via Question.answers orphanRemoval).
    @PutMapping("/forms/{id}")
    public ResponseEntity<?> updateForm(@PathVariable Long id, @RequestBody FormRequest formRequest) {
        Optional<User> userOptional = resolveAuthenticatedUser();
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userOptional.get();

        Optional<Form> formOptional = formRepository.findById(id);
        if (formOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Form form = formOptional.get();

        if (form.getCreator() == null || !form.getCreator().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only edit surveys you created"));
        }

        List<QuestionRequest> questionRequests =
                formRequest.getQuestions() != null ? formRequest.getQuestions() : List.of();
        Optional<ResponseEntity<?>> validationError = validateQuestionRequests(questionRequests);
        if (validationError.isPresent()) {
            return validationError.get();
        }

        Map<Long, Question> existingById = new HashMap<>();
        for (Question question : form.getQuestions()) {
            if (question.getId() != null) {
                existingById.put(question.getId(), question);
            }
        }

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
                Question question = new Question();
                applyQuestionFields(question, questionRequest);
                question.setForm(form);
                additions.add(question);
            }
        }

        // orphanRemoval deletes removed questions (and cascaded answers). Kept questions
        // stay managed so prior responses remain attached.
        form.getQuestions().removeIf(question ->
                question.getId() != null && !keepIds.contains(question.getId()));
        form.getQuestions().addAll(additions);

        form.setName(formRequest.getName());
        form.setResponsesPublic(formRequest.getResponsesPublic() == null || formRequest.getResponsesPublic());
        Form savedForm = formRepository.save(form);
        return ResponseEntity.ok(toFormResponse(savedForm));
    }

    // Public/anonymous - no token required (see SecurityConfig permitAll for GET /forms/**).
    // This is the endpoint a form-filling page calls; the returned creatorUsername is what
    // lets the frontend show "Created by <username>" at the top of the fill-out view.
    @GetMapping("/forms/{id}")
    public ResponseEntity<FormResponse> getForm(@PathVariable Long id) {
        Optional<Form> formOptional = formRepository.findById(id);
        if (formOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(toFormResponse(formOptional.get()));
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
        if (!form.isResponsesPublic()) {
            Optional<User> userOptional = resolveAuthenticatedUser();
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Responses for this survey are private"));
            }
            User user = userOptional.get();
            if (form.getCreator() == null || !form.getCreator().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only the survey owner can view these responses"));
            }
        }

        List<Answer> savedAnswers = answerRepository.findAllByQuestionFormIdOrderByIdAsc(id);
        Map<Long, List<String>> answersByQuestionId = new LinkedHashMap<>();

        for (Answer savedAnswer : savedAnswers) {
            Question question = savedAnswer.getQuestion();
            if (question == null || question.getId() == null) continue;
            answersByQuestionId
                    .computeIfAbsent(question.getId(), key -> new ArrayList<>())
                    .add(savedAnswer.getAnswer());
        }

        List<QuestionResultsResponse> questionResults = new ArrayList<>();
        for (Question question : form.getQuestions()) {
            questionResults.add(new QuestionResultsResponse(
                    question.getId(),
                    question.getQuestionText(),
                    question.getQuestionType(),
                    question.getQuestionOptions(),
                    answersByQuestionId.getOrDefault(question.getId(), new ArrayList<>())
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
    public ResponseEntity<List<AnswerResponse>> submitAnswers(@PathVariable Long id,
                                                                @RequestBody AnswerSubmissionRequest submissionRequest) {
        Optional<Form> formOptional = formRepository.findById(id);
        if (formOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (submissionRequest.getAnswers() == null || submissionRequest.getAnswers().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Resolve every AnswerRequest into a real Question first (and validate it) before
        // saving anything, so a single bad questionId rejects the whole submission instead
        // of partially saving answers.
        List<Answer> answersToSave = new ArrayList<>();
        for (AnswerRequest answerRequest : submissionRequest.getAnswers()) {
            if (answerRequest.getQuestionId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            Optional<Question> questionOptional = questionRepository.findById(answerRequest.getQuestionId());
            if (questionOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            Question question = questionOptional.get();

            // Guard against a client submitting an answer for a question that belongs to a
            // different form than the one in the URL (e.g. copy-pasted/forged questionId).
            if (question.getForm() == null || !question.getForm().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            Answer answer = new Answer();
            answer.setAnswer(answerRequest.getAnswer());
            answer.setQuestion(question);
            answersToSave.add(answer);
        }

        List<Answer> savedAnswers = answerRepository.saveAll(answersToSave);

        List<AnswerResponse> answerResponses = new ArrayList<>();
        for (Answer savedAnswer : savedAnswers) {
            answerResponses.add(new AnswerResponse(
                    savedAnswer.getId(),
                    savedAnswer.getQuestion().getId(),
                    savedAnswer.getAnswer()
            ));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(answerResponses);
    }

    private Optional<User> resolveAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
    }

    // Returns empty when valid; otherwise a ready-to-return error ResponseEntity.
    private Optional<ResponseEntity<?>> validateQuestionRequests(List<QuestionRequest> questionRequests) {
        for (QuestionRequest questionRequest : questionRequests) {
            if (!QuestionType.isValid(questionRequest.getQuestionType())) {
                return Optional.of(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "questionType must be 0 (SKIPPABLE_TEXT), 1 (TEXT), 2 (MULTIPLE_CHOICE), or 3 (RATING)")));
            }

            Integer type = questionRequest.getQuestionType();
            String options = questionRequest.getQuestionOptions();
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

    private void applyQuestionFields(Question question, QuestionRequest questionRequest) {
        question.setQuestionText(questionRequest.getQuestionText());
        question.setQuestionType(questionRequest.getQuestionType());
        question.setQuestionOptions(questionRequest.getQuestionOptions());
    }

    // Shared mapping used by create/update/get so responses always have the same shape,
    // including creatorUsername.
    private FormResponse toFormResponse(Form form) {
        List<QuestionResponse> questionResponses = new ArrayList<>();
        for (Question question : form.getQuestions()) {
            questionResponses.add(new QuestionResponse(
                    question.getId(),
                    question.getQuestionText(),
                    question.getQuestionType(),
                    question.getQuestionOptions()
            ));
        }

        return new FormResponse(
                form.getId(),
                form.getName(),
                form.isResponsesPublic(),
                questionResponses,
                form.getCreator().getUsername()
        );
    }
}
