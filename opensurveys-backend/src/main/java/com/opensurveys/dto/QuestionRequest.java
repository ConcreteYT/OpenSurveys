package com.opensurveys.dto;

// One question inside a FormRequest.questions list - mirrors QUESTION's non-FK columns
// (questiontext, questiontype, questionoptions). FormController maps each of these into a
// Question entity attached to the new Form.
//
// Optional `id`: on PUT /forms/{id}, when set to an existing question on that form, the
// question is updated in place (keeps answers). Omit / null to create a new question.
// Ignored on POST /forms (create).
//
// questionType must be one of QuestionType:
// 0=SKIPPABLE_TEXT (no answer field), 1=TEXT (typed answer),
// 2=MULTIPLE_CHOICE, 3=RATING (stars 1..max).
// questionOptions: unused for SKIPPABLE_TEXT/TEXT;
// MULTIPLE_CHOICE: "OptA;OptB;OptC" or "OptA;OptB;OptC;2!" (N! = required selections, default 1);
// RATING: max stars (whole number 5-10).
public class QuestionRequest {

    private Long id;
    private String questionText;
    private Integer questionType;
    private String questionOptions;

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
