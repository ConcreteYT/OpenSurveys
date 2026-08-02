package com.RTGpoll.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// A single answer to a Question. Brand-new entity introduced solely to match the provided
// schema - no controller/endpoint submits or reads these yet (that's future work, e.g. a
// form-submission flow like POST /forms/{id}/answers); this just establishes the
// persistence shape so the DB structure matches what was specified.
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

    @Column(name = "answer")
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
