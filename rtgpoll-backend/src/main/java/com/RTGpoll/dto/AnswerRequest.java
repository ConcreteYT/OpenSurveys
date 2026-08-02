package com.RTGpoll.dto;

// One answer inside an AnswerSubmissionRequest.answers list - sent by the client after it
// finishes filling out a form. questionId tells the backend which previously-fetched
// QuestionResponse.id this answer belongs to, so FormController#submitAnswers can look up
// the matching Question entity and link the new Answer to it.
public class AnswerRequest {

    private Long questionId;
    private String answer;

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
