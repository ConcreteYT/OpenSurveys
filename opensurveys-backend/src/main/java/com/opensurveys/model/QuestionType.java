package com.opensurveys.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Integer codes stored in QUESTION.questiontype. Kept as ints (not a JPA enum) so the
// column stays a plain INT matching the schema; this class is the single source of truth
// for which values are allowed when creating/reading forms.
public final class QuestionType {

    // Display-only prompt / info text. No answer is collected. questionOptions unused.
    public static final int SKIPPABLE_TEXT = 0;

    // Required free-text answer (user types their own response). questionOptions unused.
    public static final int TEXT = 1;

    // Multiple choice. questionOptions is semicolon-separated labels, optionally ending
    // with "N!" for how many must be selected (default 1). Examples:
    //   "Math;Physics;Informatik"       -> pick 1 of 3
    //   "Math;Physics;Informatik;2!"    -> pick 2 of 3
    //   "Art;Music;Karate;Biology;3!"   -> pick 3 of 4
    public static final int MULTIPLE_CHOICE = 2;

    // Star rating from 1 to maxStars. questionOptions is the max as a whole number
    // between RATING_MAX_MIN and RATING_MAX_MAX inclusive (e.g. "5" or "10").
    public static final int RATING = 3;

    public static final int RATING_MAX_MIN = 5;
    public static final int RATING_MAX_MAX = 10;

    private static final Pattern SELECT_COUNT_SUFFIX = Pattern.compile("^(\\d+)!$");

    private QuestionType() {
    }

    public static boolean isValid(Integer type) {
        return type != null
                && (type == SKIPPABLE_TEXT
                || type == TEXT
                || type == MULTIPLE_CHOICE
                || type == RATING);
    }

    // Parses questionOptions for RATING questions. Returns null if missing/invalid.
    public static Integer parseRatingMax(String questionOptions) {
        if (questionOptions == null || questionOptions.isBlank()) {
            return null;
        }
        try {
            int max = Integer.parseInt(questionOptions.trim());
            if (max < RATING_MAX_MIN || max > RATING_MAX_MAX) {
                return null;
            }
            return max;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Parses questionOptions for MULTIPLE_CHOICE. Returns null if missing/invalid.
    public static MultipleChoiceSpec parseMultipleChoice(String questionOptions) {
        if (questionOptions == null || questionOptions.isBlank()) {
            return null;
        }

        String[] rawParts = questionOptions.split(";", -1);
        List<String> parts = new ArrayList<>();
        for (String raw : rawParts) {
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }

        if (parts.isEmpty()) {
            return null;
        }

        int selectCount = 1;
        String last = parts.get(parts.size() - 1);
        Matcher matcher = SELECT_COUNT_SUFFIX.matcher(last);
        if (matcher.matches()) {
            selectCount = Integer.parseInt(matcher.group(1));
            parts.remove(parts.size() - 1);
        }

        if (parts.isEmpty()) {
            return null;
        }
        if (selectCount < 1 || selectCount > parts.size()) {
            return null;
        }

        return new MultipleChoiceSpec(List.copyOf(parts), selectCount);
    }

    public static final class MultipleChoiceSpec {
        private final List<String> options;
        private final int selectCount;

        public MultipleChoiceSpec(List<String> options, int selectCount) {
            this.options = options;
            this.selectCount = selectCount;
        }

        public List<String> getOptions() {
            return Collections.unmodifiableList(options);
        }

        public int getSelectCount() {
            return selectCount;
        }
    }
}
