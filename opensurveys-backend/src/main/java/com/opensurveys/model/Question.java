package com.opensurveys.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

// A single question belonging to a Form. Replaces the old placeholder "Frage" entity now
// that the real schema is known - a Form can now have many Questions instead of just one.
//
// Maps onto the provided schema's QUESTION table:
//   QUESTION(question_id PK autoincrement, questiontext, questiontype (int),
//            questionoptions, FORM_form_id FK -> FORM.form_id)
// Built by FormController#createForm from the FormRequest.questions list, and read back
// (via FormController#getForm) as part of FormResponse.questions for anonymous fillers.
@Entity
@Table(name = "QUESTION")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    @Column(name = "questiontext")
    private String questionText;

    // Integer code from QuestionType:
    // 0=SKIPPABLE_TEXT (display only, no answer), 1=TEXT (typed answer),
    // 2=MULTIPLE_CHOICE, 3=RATING (stars 1..max).
    // Stored as INT per the schema (not a JPA enum) so the DB column stays a bare integer.
    @Column(name = "questiontype")
    private Integer questionType;

    // Depends on questionType:
    //  - SKIPPABLE_TEXT (0) / TEXT (1): unused / may be null
    //  - MULTIPLE_CHOICE (2): semicolon-separated choices, optional trailing "N!"
    //    (e.g. "Math;Physics;Informatik;2!" means pick 2 of 3; without N! pick 1)
    //  - RATING (3): max star count as a whole number from 5 to 10 (e.g. "5" or "10")
    // Single string column matches the schema (no separate options table).
    @Column(name = "questionoptions")
    private String questionOptions;

    // Owning side of the Form<->Question relationship - the FK column FORM_form_id lives
    // here. Set by FormController before the Form (and its cascaded Questions) is saved.
    @ManyToOne
    @JoinColumn(name = "FORM_form_id")
    private Form form;

    // Inverse side of ANSWER.QUESTION_question_id. Not wired to any controller/endpoint yet -
    // answer submission (e.g. a future POST /forms/{id}/answers route) is out of scope for
    // now; this only establishes the persistence shape described by the schema.
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

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

    public Form getForm() {
        return form;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }
}
