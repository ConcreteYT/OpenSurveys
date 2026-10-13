package com.opensurveys.service;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 6;

    private PasswordPolicy() {
    }

    public static boolean isTooShort(String password) {
        return password == null || password.length() < MIN_LENGTH;
    }
}
