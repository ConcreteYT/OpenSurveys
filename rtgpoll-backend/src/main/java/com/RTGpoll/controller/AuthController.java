package com.RTGpoll.controller;

import com.RTGpoll.dto.AuthResponse;
import com.RTGpoll.dto.GoogleLoginRequest;
import com.RTGpoll.dto.LoginRequest;
import com.RTGpoll.dto.RegisterRequest;
import com.RTGpoll.model.User;
import com.RTGpoll.repository.UserRepository;
import com.RTGpoll.security.GoogleTokenVerifier;
import com.RTGpoll.security.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

// Public entry point for account creation and sign-in (both permitAll in SecurityConfig).
// This is where the "sign in or register only when creating a form" requirement is
// satisfied on the client side: a client must call one of these endpoints to obtain
// a JWT before it can successfully call FormController#createForm (POST /forms).
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    // Creates the User record (password hashed via the BCryptPasswordEncoder bean from
    // SecurityConfig) and immediately returns a JWT, so the client is auto-logged-in and
    // can go straight on to POST /forms without a separate login call.
    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        Optional<User> existing = userRepository.findByUsername(request.getUsername());
        if (existing.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "username already taken"));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.ROLE_USER);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, user.getRole(), user.getUsername()));
    }

    // Verifies credentials against the stored BCrypt hash and, on success, issues a fresh
    // JWT (via JwtUtil) that the client attaches as `Authorization: Bearer <token>` on
    // subsequent requests - JwtAuthFilter is what reads that header back on each request.
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid username or password"));
        }

        User user = userOpt.get();
        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid username or password"));
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, user.getRole(), user.getUsername()));
    }

    /**
     * Google Sign-In: verify the ID token, find or create/link the user, return a JWT.
     * Existing password accounts with the same verified email are linked (googleId set).
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());
        if (payload == null) {
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
            String token = jwtUtil.generateToken(user.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, user.getRole(), user.getUsername()));
        }

        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            if (user.getGoogleId() != null && !user.getGoogleId().equals(googleId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "email is already linked to a different Google account"));
            }
            // Link Google to the existing password (or previously unlinked) account.
            user.setGoogleId(googleId);
            if (user.getName() == null || user.getName().isBlank()) {
                user.setName(name);
            }
            userRepository.save(user);
            String token = jwtUtil.generateToken(user.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, user.getRole(), user.getUsername()));
        }

        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName(name);
        user.setUsername(uniqueUsernameFromEmail(email));
        user.setPassword(null);
        user.setRole(User.ROLE_USER);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, user.getRole(), user.getUsername()));
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
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }
}
