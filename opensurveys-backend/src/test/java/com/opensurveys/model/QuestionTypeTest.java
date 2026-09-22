package com.opensurveys.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionTypeTest {

    @Test
    void imageUploadIsValidType() {
        assertTrue(QuestionType.isValid(QuestionType.IMAGE_UPLOAD));
        assertFalse(QuestionType.isValid(99));
    }

    @Test
    void parseImageUploadMaxFiles_acceptsOneThroughTen() {
        assertEquals(1, QuestionType.parseImageUploadMaxFiles("1"));
        assertEquals(10, QuestionType.parseImageUploadMaxFiles("10"));
        assertNull(QuestionType.parseImageUploadMaxFiles("0"));
        assertNull(QuestionType.parseImageUploadMaxFiles("11"));
        assertNull(QuestionType.parseImageUploadMaxFiles("x"));
    }
}
