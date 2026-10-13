package com.opensurveys.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.opensurveys.dto.AuthResponse;
import com.opensurveys.dto.GoogleLoginRequest;
import com.opensurveys.dto.LoginRequest;
import com.opensurveys.dto.RegisterRequest;
import com.opensurveys.dto.VerifyEmailRequest;
import com.opensurveys.model.User;
import com.opensurveys.model.VerificationCode;
import com.opensurveys.repository.UserRepository;
import com.opensurveys.security.AuthCookieService;
import com.opensurveys.security.ClientRateLimiter;
import com.opensurveys.security.GoogleTokenVerifier;
import com.opensurveys.service.PasswordPolicy;
import com.opensurveys.service.VerificationCodeService;
import com.opensurveys.service.VerificationCodeService.ConsumeResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String GENERIC_SIGNUP_ERROR = "could not create account";
    private static final long TEN_MINUTES_MS = 10 * 60 * 1000L;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    @Autowired
    private AuthCookieService authCookieService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private ClientRateLimiter rateLimiter;

    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        if (!rateLimiter.allow(clientKey(httpRequest, "signup"), 5, TEN_MINUTES_MS)) {
            return tooManyRequests();
        }

        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String name = request.getName() == null ? "" : request.getName().trim();
        if (username.isEmpty() || username.length() > 64) {
            return ResponseEntity.badRequest().body(Map.of("error", "username is required"));
        }
        if (!isPlausibleEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "email is required"));
        }
        if (PasswordPolicy.isTooShort(request.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "password must be at least 6 characters"));
        }
        if (userRepository.existsByUsername(username) || userRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", GENERIC_SIGNUP_ERROR));
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.ROLE_USER);
        user.setEmailVerified(false);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", GENERIC_SIGNUP_ERROR));
        }

        try {
            verificationCodeService.sendCode(user, VerificationCode.PURPOSE_SIGNUP);
        } catch (VerificationCodeService.MailSendFailedException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage(), "verificationRequired", true, "username", user.getUsername()));
        } catch (VerificationCodeService.LockedOutException e) {
            return locked();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.verificationRequired(user.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        if (!rateLimiter.allow(clientKey(httpRequest, "login"), 10, 60_000L)) {
            return tooManyRequests();
        }

        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            return invalidCredentials();
        }

        User user = userOpt.get();
        if (user.getPassword() == null
                || request.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return invalidCredentials();
        }
        if (!user.isEmailVerified()) {
            return ResponseEntity.ok(AuthResponse.verificationRequired(user.getUsername()));
        }

        authCookieService.write(httpResponse, user);
        return ResponseEntity.ok(new AuthResponse(user.getRole(), user.getUsername()));
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request, HttpServletResponse httpResponse) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());
        if (payload == null || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid Google ID token"));
        }

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Google account has no email"));
        }
        email = email.trim();

        String name = (String) payload.get("name");
        if (name == null || name.isBlank()) {
            name = email;
        }

        Optional<User> byGoogleId = userRepository.findByGoogleId(googleId);
        if (byGoogleId.isPresent()) {
            User user = byGoogleId.get();
            if (!user.isEmailVerified()) {
                return ResponseEntity.ok(AuthResponse.verificationRequired(user.getUsername()));
            }
            authCookieService.write(httpResponse, user);
            return ResponseEntity.ok(new AuthResponse(user.getRole(), user.getUsername()));
        }

        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            if (!user.isEmailVerified()) {
                return ResponseEntity.ok(AuthResponse.verificationRequired(user.getUsername()));
            }
            if (user.getGoogleId() != null && !user.getGoogleId().equals(googleId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "email is already linked to a different Google account"));
            }
            user.setGoogleId(googleId);
            if (user.getName() == null || user.getName().isBlank()) {
                user.setName(name);
            }
            userRepository.save(user);
            authCookieService.write(httpResponse, user);
            return ResponseEntity.ok(new AuthResponse(user.getRole(), user.getUsername()));
        }

        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName(name);
        user.setUsername(uniqueUsernameFromEmail(email));
        user.setPassword(null);
        user.setRole(User.ROLE_USER);
        user.setEmailVerified(true);
        userRepository.save(user);

        authCookieService.write(httpResponse, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(user.getRole(), user.getUsername()));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyEmailRequest request, HttpServletResponse httpResponse) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username is required"));
        }
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername().trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid or expired verification code"));
        }
        User user = userOpt.get();
        ConsumeResult result = verificationCodeService.consumeCode(user, VerificationCode.PURPOSE_SIGNUP, request.getCode());
        if (result == ConsumeResult.LOCKED) {
            return locked();
        }
        if (result != ConsumeResult.OK) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid or expired verification code"));
        }
        user.setEmailVerified(true);
        userRepository.save(user);
        authCookieService.write(httpResponse, user);
        return ResponseEntity.ok(new AuthResponse(user.getRole(), user.getUsername()));
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resend(@RequestBody VerifyEmailRequest request, HttpServletRequest httpRequest) {
        if (!rateLimiter.allow(clientKey(httpRequest, "resend"), 5, TEN_MINUTES_MS)) {
            return tooManyRequests();
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username is required"));
        }
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername().trim());
        if (userOpt.isEmpty() || userOpt.get().isEmailVerified()) {
            return ResponseEntity.ok(Map.of("message", "if the account needs verification, a code was sent"));
        }
        try {
            verificationCodeService.sendCode(userOpt.get(), VerificationCode.PURPOSE_SIGNUP);
            return ResponseEntity.ok(Map.of("message", "if the account needs verification, a code was sent"));
        } catch (VerificationCodeService.MailSendFailedException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (VerificationCodeService.LockedOutException e) {
            return locked();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse httpResponse) {
        authCookieService.clear(httpResponse);
        return ResponseEntity.noContent().build();
    }

    private String uniqueUsernameFromEmail(String email) {
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String base = local.replaceAll("[^a-zA-Z0-9._-]", "");
        if (base.isBlank()) {
            base = "user";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private static boolean isPlausibleEmail(String email) {
        if (email.length() > 254 || email.length() < 3) {
            return false;
        }
        int at = email.indexOf('@');
        return at > 0 && at < email.length() - 1 && email.indexOf('@', at + 1) < 0 && email.indexOf('.') > at;
    }

    private static String clientKey(HttpServletRequest request, String action) {
        return action + ":" + request.getRemoteAddr();
    }

    private static ResponseEntity<?> tooManyRequests() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", "too many requests"));
    }

    private static ResponseEntity<?> invalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid username or password"));
    }

    private static ResponseEntity<?> locked() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "too many attempts; try again in 24 hours"));
    }
}
