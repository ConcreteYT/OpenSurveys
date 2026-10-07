package com.opensurveys.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "test-secret-that-is-definitely-long-enough-for-hs256";

    @Test
    void roundTripsUsername() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);
        String token = jwtUtil.generateToken("alice");
        assertEquals(Optional.of("alice"), jwtUtil.parseUsername(token));
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        String token = new JwtUtil(SECRET + "-other", 60_000).generateToken("alice");
        assertTrue(new JwtUtil(SECRET, 60_000).parseUsername(token).isEmpty());
    }

    @Test
    void rejectsExpiredToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1_000);
        assertTrue(jwtUtil.parseUsername(jwtUtil.generateToken("alice")).isEmpty());
    }

    @Test
    void rejectsGarbageWithoutThrowing() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);
        assertTrue(jwtUtil.parseUsername("not-a-jwt").isEmpty());
        assertTrue(jwtUtil.parseUsername("").isEmpty());
        assertTrue(jwtUtil.parseUsername("eyJ.bad.token").isEmpty());
    }
}
