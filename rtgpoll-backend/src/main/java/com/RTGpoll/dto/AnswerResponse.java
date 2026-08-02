package com.RTGpoll.dto;

// One saved answer, returned by FormController#submitAnswers as confirmation of what was
// persisted to the ANSWER table. Mirrors Answer's non-relationship field (answer text) plus
// the generated id and the questionId it was linked to, so the client can double check
// every submitted answer was stored against the question it intended.
public class AnswerResponse {

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
