package com.opensurveys.dto;

import com.opensurveys.model.Answer;

import java.util.List;

// One saved answer, returned by FormController#submitAnswers and
// FormSubmissionController#commitSubmission as confirmation of what was persisted to the
// ANSWER table. Mirrors Answer's non-relationship field (answer text) plus the generated id
// and the questionId it was linked to, so the client can double check every submitted
// answer was stored against the question it intended.
public class AnswerResponse {

    public static AnswerResponse from(Answer answer) {
        return new AnswerResponse(answer.getId(), answer.getQuestion().getId(), answer.getAnswer());
    }

    public static List<AnswerResponse> fromAll(List<Answer> answers) {
        return answers.stream().map(AnswerResponse::from).toList();
    }

    private Long id;
    private Long questionId;
    private String answer;

    public AnswerResponse() {
    }

    public AnswerResponse(Long id, Long questionId, String answer) {
        this.id = id;
        this.questionId = questionId;
        this.answer = answer;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
