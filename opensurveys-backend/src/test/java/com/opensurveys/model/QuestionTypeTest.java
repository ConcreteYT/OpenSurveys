package com.opensurveys.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void parseImageUploadMaxFiles_trimsAndRejectsBlank() {
        assertEquals(3, QuestionType.parseImageUploadMaxFiles(" 3 "));
        assertNull(QuestionType.parseImageUploadMaxFiles(null));
        assertNull(QuestionType.parseImageUploadMaxFiles("  "));
        assertNull(QuestionType.parseImageUploadMaxFiles("2.5"));
    }

    @Test
    void parseRatingMax_acceptsFiveThroughTen() {
        assertEquals(5, QuestionType.parseRatingMax("5"));
        assertEquals(10, QuestionType.parseRatingMax(" 10 "));
        assertNull(QuestionType.parseRatingMax("4"));
        assertNull(QuestionType.parseRatingMax("11"));
        assertNull(QuestionType.parseRatingMax(null));
        assertNull(QuestionType.parseRatingMax("five"));
    }

    @Test
    void is_isNullSafe() {
        assertTrue(QuestionType.is(QuestionType.IMAGE_UPLOAD, QuestionType.IMAGE_UPLOAD));
        assertFalse(QuestionType.is(QuestionType.TEXT, QuestionType.IMAGE_UPLOAD));
        assertFalse(QuestionType.is(null, QuestionType.SKIPPABLE_TEXT));
    }

    @Test
    void parseMultipleChoice_handlesSelectCountSuffix() {
        QuestionType.MultipleChoiceSpec single = QuestionType.parseMultipleChoice("Math; Physics ;Art");
        assertNotNull(single);
        assertEquals(List.of("Math", "Physics", "Art"), single.getOptions());
        assertEquals(1, single.getSelectCount());

        QuestionType.MultipleChoiceSpec two = QuestionType.parseMultipleChoice("Math;Physics;Art;2!");
        assertNotNull(two);
        assertEquals(List.of("Math", "Physics", "Art"), two.getOptions());
        assertEquals(2, two.getSelectCount());

        assertNull(QuestionType.parseMultipleChoice("a;b;3!"));
        assertNull(QuestionType.parseMultipleChoice("a;0!"));
        assertNull(QuestionType.parseMultipleChoice("2!"));
        assertNull(QuestionType.parseMultipleChoice(" ; "));
        assertNull(QuestionType.parseMultipleChoice(null));
    }

    @Test
    void multipleChoiceOptionsAreUnmodifiable() {
        QuestionType.MultipleChoiceSpec spec = QuestionType.parseMultipleChoice("a;b");
        assertNotNull(spec);
        assertThrows(UnsupportedOperationException.class, () -> spec.getOptions().add("c"));

        List<String> source = new java.util.ArrayList<>(List.of("x", "y"));
        QuestionType.MultipleChoiceSpec direct = new QuestionType.MultipleChoiceSpec(source, 1);
        source.add("z");
        assertEquals(List.of("x", "y"), direct.getOptions());
    }
}
