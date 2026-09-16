package com.opensurveys.dto;

import java.util.List;

/**
 * Per-question slice of form results: question metadata plus every submitted
 * answer string (used by the frontend to build MCQ/rating aggregates).
 */
public class QuestionResultsResponse {

    private Long id;
    private String questionText;
    private Integer questionType;
    private String questionOptions;
    /** Raw answer values for this question, in submission order. */
    private List<String> answers;

    public QuestionResultsResponse() {
    }

    public QuestionResultsResponse(Long id, String questionText, Integer questionType, String questionOptions, List<String> answers) {
        this.id = id;
        this.questionText = questionText;
        this.questionType = questionType;
        this.questionOptions = questionOptions;
        this.answers = answers;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Integer getQuestionType() {
        return questionType;
    }

    public void setQuestionType(Integer questionType) {
        this.questionType = questionType;
    }

    public String getQuestionOptions() {
        return questionOptions;
    }

    public void setQuestionOptions(String questionOptions) {
        this.questionOptions = questionOptions;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }
}
