package com.opensurveys.service;

import java.security.SecureRandom;
import java.util.function.Predicate;

/**
 * 8-digit survey access codes. Stored as text so leading zeros are preserved.
 */
public final class AccessCodes {

    public static final int LENGTH = 8;
    public static final int BOUND = 100_000_000;
    public static final int MAX_ATTEMPTS = 10;

    private AccessCodes() {
    }

    public static String format(int value) {
        if (value < 0 || value >= BOUND) {
            throw new IllegalArgumentException("access code out of range");
        }
        return String.format("%08d", value);
    }

    public static String generate(SecureRandom random) {
        return format(random.nextInt(BOUND));
    }

    public static boolean isValid(String code) {
        if (code == null || code.length() != LENGTH) {
            return false;
        }
        for (int i = 0; i < LENGTH; i++) {
            char c = code.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * @param taken true when that code is already assigned
     */
    public static String allocate(SecureRandom random, Predicate<String> taken) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = generate(random);
            if (!taken.test(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not allocate a unique access code");
    }
}
