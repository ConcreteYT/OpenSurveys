package com.opensurveys.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opensurveys.model.Form;

import java.util.List;

// Response body for FormController's list/create/update/get endpoints.
// creatorUsername is the key field for anonymous form-filling: it's populated from
// Form.creator.getUsername() so a client can display "Created by <creatorUsername>"
// at the top of the fill-out page without needing to be logged in itself.
// `questions` mirrors Form.questions, mapped to QuestionResponse so the FK/back-reference
// to the Form entity itself isn't serialized (avoids infinite recursion / leaking internals).
public class FormResponse {

    public static FormResponse from(Form form) {
        return from(form, true, false);
    }

    public static FormResponse forRespondent(Form form, boolean alreadySubmitted) {
        return from(form, false, alreadySubmitted);
    }

    private static FormResponse from(Form form, boolean includeAccessCode, boolean alreadySubmitted) {
        List<QuestionResponse> questionResponses = form.getQuestions().stream()
                .map(question -> new QuestionResponse(
                        question.getId(),
                        question.getQuestionText(),
                        question.getQuestionType(),
                        question.getQuestionOptions()))
                .toList();

        FormResponse response = new FormResponse(
                form.getId(),
                includeAccessCode ? form.getAccessCode() : null,
                form.getName(),
                form.isResponsesPublic(),
                form.isOneResponseOnly(),
                questionResponses,
                form.getCreator().getUsername()
        );
        response.setAlreadySubmitted(alreadySubmitted);
        return response;
    }

    private Long id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String accessCode;
    private boolean alreadySubmitted;
    private String name;
    private boolean responsesPublic;
    private boolean oneResponseOnly;
    private List<QuestionResponse> questions;
    private String creatorUsername;

    public FormResponse() {
    }

    public FormResponse(Long id, String accessCode, String name, boolean responsesPublic, boolean oneResponseOnly,
                        List<QuestionResponse> questions, String creatorUsername) {
        this.id = id;
        this.accessCode = accessCode;
        this.name = name;
        this.responsesPublic = responsesPublic;
        this.oneResponseOnly = oneResponseOnly;
        this.questions = questions;
        this.creatorUsername = creatorUsername;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public boolean isAlreadySubmitted() {
        return alreadySubmitted;
    }

    public void setAlreadySubmitted(boolean alreadySubmitted) {
        this.alreadySubmitted = alreadySubmitted;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isResponsesPublic() {
        return responsesPublic;
    }

    public void setResponsesPublic(boolean responsesPublic) {
        this.responsesPublic = responsesPublic;
    }

    public boolean isOneResponseOnly() {
        return oneResponseOnly;
    }

    public void setOneResponseOnly(boolean oneResponseOnly) {
        this.oneResponseOnly = oneResponseOnly;
    }

    public List<QuestionResponse> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionResponse> questions) {
        this.questions = questions;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }
}
