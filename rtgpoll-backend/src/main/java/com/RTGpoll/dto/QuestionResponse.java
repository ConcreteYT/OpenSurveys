package com.RTGpoll.dto;

// One question inside a FormResponse.questions list - mirrors Question's non-relationship
// fields (id + the schema's questiontext/questiontype/questionoptions columns). Built by
// FormController#toFormResponse from a saved/fetched Question entity.
// questionType: 0=SKIPPABLE_TEXT, 1=TEXT, 2=MULTIPLE_CHOICE, 3=RATING (see QuestionType).
// For RATING, questionOptions is the max star count (5-10).
// For MULTIPLE_CHOICE, semicolon-separated options with optional trailing N!.
public class QuestionResponse {

    private Long id;
    private String questionText;
    private Integer questionType;
    private String questionOptions;

    public QuestionResponse() {
    }

    public QuestionResponse(Long id, String questionText, Integer questionType, String questionOptions) {
        this.id = id;
        this.questionText = questionText;
        this.questionType = questionType;
        this.questionOptions = questionOptions;
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
}
