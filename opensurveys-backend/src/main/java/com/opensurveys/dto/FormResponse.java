package com.opensurveys.dto;

import java.util.List;

// Response body for both FormController endpoints (create and public get-by-id).
// creatorUsername is the key field for anonymous form-filling: it's populated from
// Form.creator.getUsername() so a client can display "Created by <creatorUsername>"
// at the top of the fill-out page without needing to be logged in itself.
// `questions` mirrors Form.questions, mapped to QuestionResponse so the FK/back-reference
// to the Form entity itself isn't serialized (avoids infinite recursion / leaking internals).
public class FormResponse {

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
