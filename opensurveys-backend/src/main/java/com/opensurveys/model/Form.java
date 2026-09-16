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
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

// A single poll/form. Created exclusively through FormController#createForm (POST /forms,
// requires a valid JWT), which sets `creator` to the authenticated User resolved from
// SecurityContextHolder. Read back publicly (no auth) via FormController#getForm
// (GET /forms/{id}), which maps this entity to a FormResponse containing
// creator.getUsername() so an anonymous filler can see who made the form.
//
// Maps onto the provided schema's FORM table:
//   FORM(form_id PK autoincrement, name, USER_user_id FK -> USER.user_id)
// The one-to-many link to Question is the inverse side of QUESTION.FORM_form_id
// (the FK column actually lives on QUESTION, see Question.form below).
@Entity
@Table(name = "FORM")
public class Form {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "form_id")
    private Long id;

    @Column(name = "name")
    private String name;

    // When true (default), anyone with the form id can load aggregated answers via
    // GET /forms/{id}/responses. When false, only the form's creator may view responses.
    @ColumnDefault("true")
    @Column(name = "responses_public", nullable = false)
    private boolean responsesPublic = true;

    // FK column USER_user_id on FORM, pointing at whoever created this form.
    // Never null for forms created via the API (FormController rejects unauthenticated
    // create attempts before a Form is ever built).
    @ManyToOne
    @JoinColumn(name = "USER_user_id")
    private User creator;

    // Inverse side of the relationship - Question owns the FK (FORM_form_id), so changes
    // to this list only take effect because of cascade=ALL below (e.g. FormController adds
    // Questions here before saving the Form, and JPA cascades the insert to QUESTION rows).
    // orphanRemoval=true means removing a Question from this list deletes its row too.
    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isResponsesPublic() {
        return responsesPublic;
    }

    public void setResponsesPublic(boolean responsesPublic) {
        this.responsesPublic = responsesPublic;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}
