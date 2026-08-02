package com.RTGpoll.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// Central place for issuing and validating JWTs. Two consumers:
//  - AuthController calls generateToken() after a successful register/login and returns
//    it to the client as the bearer token.
//  - JwtAuthFilter calls isTokenValid() then extractUsername() on every incoming request
//    to decide whether/whom to authenticate.
// Signing secret/expiry come from application.properties (jwt.secret, jwt.expiration-ms).
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    // Same secret -> same key every call, so tokens issued by generateToken() can later
    // be verified by extractUsername()/isTokenValid() (possibly after an app restart).
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Subject = username, so JwtAuthFilter can look the user up via UserRepository#findByUsername.
    public String generateToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    // Only safe to call after isTokenValid() has confirmed the signature/expiry (JwtAuthFilter
    // always checks isTokenValid() first).
    public String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    // Never throws - any malformed/expired/tampered token just fails validation so
    // JwtAuthFilter can fall through and treat the request as anonymous.
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
