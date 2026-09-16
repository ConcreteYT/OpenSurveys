package com.opensurveys.dto;

import java.util.List;

/**
 * Aggregated results for an entire form, returned by GET /forms/{id}/responses.
 * Contains the form identity plus per-question answer lists for charting.
 */
public class FormResultsResponse {

    private Long formId;
    private String formName;
    private List<QuestionResultsResponse> questions;

    public FormResultsResponse() {
    }

    public FormResultsResponse(Long formId, String formName, List<QuestionResultsResponse> questions) {
        this.formId = formId;
        this.formName = formName;
        this.questions = questions;
    }

    public Long getFormId() {
        return formId;
    }

    public void setFormId(Long formId) {
        this.formId = formId;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public List<QuestionResultsResponse> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionResultsResponse> questions) {
        this.questions = questions;
    }
}
