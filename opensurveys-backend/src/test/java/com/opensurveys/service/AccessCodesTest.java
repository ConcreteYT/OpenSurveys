package com.opensurveys.service;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessCodesTest {

    @Test
    void formatPreservesLeadingZeros() {
        assertEquals("00000000", AccessCodes.format(0));
        assertEquals("00000004", AccessCodes.format(4));
        assertEquals("12345678", AccessCodes.format(12_345_678));
        assertEquals(8, AccessCodes.generate(fixed(42)).length());
        assertEquals("00000042", AccessCodes.generate(fixed(42)));
        assertTrue(AccessCodes.isValid("00000004"));
        assertTrue(AccessCodes.isValid("00000000"));
        assertFalse(AccessCodes.isValid("4"));
        assertFalse(AccessCodes.isValid("0000000a"));
        assertFalse(AccessCodes.isValid("000000004"));
        assertFalse(AccessCodes.isValid(null));
    }

    @Test
    void allocateSkipsCodesAlreadyTaken() {
        SecureRandom random = new SecureRandom() {
            private int n;

            @Override
            public int nextInt(int bound) {
                return n++;
            }
        };
        Set<String> taken = new HashSet<>(Set.of("00000000", "00000001"));
        assertEquals("00000002", AccessCodes.allocate(random, taken::contains));
    }

    @Test
    void allocateFailsWhenEveryAttemptCollides() {
        assertThrows(IllegalStateException.class, () -> AccessCodes.allocate(fixed(0), code -> true));
    }

    private static SecureRandom fixed(int value) {
        return new SecureRandom() {
            @Override
            public int nextInt(int bound) {
                return value;
            }
        };
    }
}
