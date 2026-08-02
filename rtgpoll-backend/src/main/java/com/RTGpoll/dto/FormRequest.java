package com.RTGpoll.dto;

import java.util.List;

// Request body for FormController#createForm (POST /forms) and #updateForm (PUT /forms/{id}).
// `name` maps to FORM.name; `questions` becomes / syncs the Form's Question entities
// (one Question per QuestionRequest, each holding questiontext/questiontype/questionoptions).
public class FormRequest {

    private String name;
    // Null means "use default" (true on create) so older clients keep public responses.
    private Boolean responsesPublic;
    private List<QuestionRequest> questions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getResponsesPublic() {
        return responsesPublic;
    }

    public void setResponsesPublic(Boolean responsesPublic) {
        this.responsesPublic = responsesPublic;
    }

    public List<QuestionRequest> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionRequest> questions) {
        this.questions = questions;
    }
}
