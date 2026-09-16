package com.opensurveys.dto;

import java.util.List;

// Request body for FormController#submitAnswers (POST /forms/{id}/answers). This is the
// "JSON file containing the answers" sent by the client once it has finished answering all
// of a form's questions - one AnswerRequest per question, each carrying the questionId the
// client received earlier from FormResponse.questions (see QuestionResponse.id).
public class AnswerSubmissionRequest {

    private List<AnswerRequest> answers;

    public List<AnswerRequest> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerRequest> answers) {
        this.answers = answers;
    }
}
