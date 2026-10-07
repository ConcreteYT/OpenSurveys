package com.opensurveys.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormTest {

    private static Question question(Long id, Integer type) {
        Question question = new Question();
        question.setId(id);
        question.setQuestionType(type);
        return question;
    }

    @Test
    void hasImageUploadQuestions() {
        Form form = new Form();
        assertFalse(form.hasImageUploadQuestions());

        form.getQuestions().add(question(1L, QuestionType.TEXT));
        form.getQuestions().add(question(2L, null));
        assertFalse(form.hasImageUploadQuestions());

        form.getQuestions().add(question(3L, QuestionType.IMAGE_UPLOAD));
        assertTrue(form.hasImageUploadQuestions());
    }

    @Test
    void questionsByIdSkipsUnsavedQuestions() {
        Form form = new Form();
        Question saved = question(7L, QuestionType.TEXT);
        form.getQuestions().add(saved);
        form.getQuestions().add(question(null, QuestionType.TEXT));

        Map<Long, Question> byId = form.questionsById();
        assertEquals(1, byId.size());
        assertSame(saved, byId.get(7L));
    }
}
