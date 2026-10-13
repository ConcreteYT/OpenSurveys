package com.opensurveys.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

// Central place for issuing and validating JWTs. Two consumers:
//  - AuthController/AccountController call generateToken() after a successful
//    register/login/rename and return it to the client as the bearer token.
//  - JwtAuthFilter calls parseUsername() on every incoming request to decide
//    whether/whom to authenticate.
// Signing secret/expiry come from application.properties (jwt.secret, jwt.expiration-ms).
@Component
public class JwtUtil {

    // Derived from the configured secret, so tokens stay valid across app restarts.
    private final SecretKey signingKey;
    private final long expirationMs;
    private final JwtParser parser;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.parser = Jwts.parser().verifyWith(signingKey).build();
    }

    // Subject = username, so JwtAuthFilter can look the user up via UserRepository#findByUsername.
    public String generateToken(String username) {
        return generateToken(username, 0);
    }

    public String generateToken(String username, int tokenVersion) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("ver", tokenVersion)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    // Never throws - any malformed/expired/tampered token yields empty so JwtAuthFilter
    // can fall through and treat the request as anonymous.
    public Optional<String> parseUsername(String token) {
        return parse(token).map(ParsedToken::username);
    }

    public Optional<ParsedToken> parse(String token) {
        try {
            var claims = parser.parseSignedClaims(token).getPayload();
            String username = claims.getSubject();
            if (username == null || username.isBlank()) {
                return Optional.empty();
            }
            Number version = claims.get("ver", Number.class);
            return Optional.of(new ParsedToken(username, version == null ? 0 : version.intValue()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
