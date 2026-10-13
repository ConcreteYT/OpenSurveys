package com.opensurveys.service;

import com.opensurveys.model.RespondentToken;
import com.opensurveys.repository.RespondentTokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class RespondentTokenService {

    static final Duration TTL = Duration.ofHours(2);

    private final RespondentTokenRepository repository;
    private final SecureRandom random = new SecureRandom();
    private volatile long lastSweepMillis;

    public RespondentTokenService(RespondentTokenRepository repository) {
        this.repository = repository;
    }

    public static String cookieName(long formId) {
        return "osr_" + formId;
    }

    /**
     * Reuses the browser cookie when it still belongs to this form.
     * A used token stays used so the same browser cannot start another response.
     *
     * @return true when this browser already submitted
     */
    @Transactional
    public boolean prepare(long formId, boolean oneResponseOnly, HttpServletRequest request, HttpServletResponse response) {
        if (!oneResponseOnly) {
            return false;
        }
        Instant now = Instant.now();
        sweepExpired(now);

        String presented = readCookie(request, formId);
        if (presented != null) {
            Optional<RespondentToken> existing = repository.findById(hash(presented));
            if (existing.isPresent() && formId == existing.get().getFormId()) {
                RespondentToken token = existing.get();
                if (token.isUsed()) {
                    return true;
                }
                if (token.getExpiresAt().isAfter(now)) {
                    return false;
                }
            }
        }

        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        RespondentToken token = new RespondentToken();
        token.setTokenHash(hash(value));
        token.setFormId(formId);
        token.setExpiresAt(now.plus(TTL));
        token.setUsed(false);
        repository.save(token);
        writeCookie(response, formId, value);
        return false;
    }

    @Transactional
    public boolean consume(long formId, HttpServletRequest request) {
        String presented = readCookie(request, formId);
        if (presented == null) {
            return false;
        }
        return repository.markUsed(hash(presented), formId, Instant.now()) == 1;
    }

    @Transactional
    public void restore(long formId, HttpServletRequest request) {
        String presented = readCookie(request, formId);
        if (presented != null) {
            repository.markUnused(hash(presented), formId);
        }
    }

    private static String readCookie(HttpServletRequest request, long formId) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String name = cookieName(formId);
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static void writeCookie(HttpServletResponse response, long formId, String value) {
        ResponseCookie cookie = ResponseCookie.from(cookieName(formId), value)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(TTL)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void sweepExpired(Instant now) {
        long millis = now.toEpochMilli();
        if (millis - lastSweepMillis < 60_000L) {
            return;
        }
        lastSweepMillis = millis;
        repository.deleteExpired(now);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
