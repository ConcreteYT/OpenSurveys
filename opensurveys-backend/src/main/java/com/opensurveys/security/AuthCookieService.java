package com.opensurveys.security;

import com.opensurveys.model.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class AuthCookieService {

    public static final String COOKIE_NAME = "opensurveys_session";

    private final JwtUtil jwtUtil;
    private final boolean secure;

    public AuthCookieService(JwtUtil jwtUtil, @Value("${app.cookie.secure:false}") boolean secure) {
        this.jwtUtil = jwtUtil;
        this.secure = secure;
    }

    public void write(HttpServletResponse response, User user) {
        String token = jwtUtil.generateToken(user.getUsername(), user.getTokenVersion());
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(token, Duration.ofMillis(jwtUtil.getExpirationMs())).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie("", Duration.ZERO).toString());
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private ResponseCookie sessionCookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
    }
}
