package com.opensurveys.dto;

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
        List<QuestionResponse> questionResponses = form.getQuestions().stream()
                .map(question -> new QuestionResponse(
                        question.getId(),
                        question.getQuestionText(),
                        question.getQuestionType(),
                        question.getQuestionOptions()))
                .toList();

        return new FormResponse(
                form.getId(),
                form.getName(),
                form.isResponsesPublic(),
                questionResponses,
                form.getCreator().getUsername()
        );
    }

    private Long id;
    private String name;
    private boolean responsesPublic;
    private List<QuestionResponse> questions;
    private String creatorUsername;

    public FormResponse() {
    }

    public FormResponse(Long id, String name, boolean responsesPublic,
                        List<QuestionResponse> questions, String creatorUsername) {
        this.id = id;
        this.name = name;
        this.responsesPublic = responsesPublic;
        this.questions = questions;
        this.creatorUsername = creatorUsername;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
