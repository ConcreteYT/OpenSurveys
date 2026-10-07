package com.opensurveys.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// A single answer to a Question. Written by FormController#submitAnswers and
// FormSubmissionController#commitSubmission, read back for GET /forms/{id}/responses,
// media access checks and the export.
//
// Maps onto the provided schema's ANSWER table:
//   ANSWER(answer_id PK autoincrement, answer, QUESTION_question_id FK -> QUESTION.question_id)
@Entity
@Table(name = "ANSWER")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long id;

    // TEXT so IMAGE_UPLOAD answers (semicolon-separated stored filenames) and long text replies fit.
    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    // Owning side of the Question<->Answer relationship - FK column QUESTION_question_id
    // lives here (see Question.answers for the inverse side).
    @ManyToOne
    @JoinColumn(name = "QUESTION_question_id")
    private Question question;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }
}
