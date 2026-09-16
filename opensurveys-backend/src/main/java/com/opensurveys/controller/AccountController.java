package com.opensurveys.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opensurveys.dto.AccountProfileResponse;
import com.opensurveys.dto.DeleteAccountRequest;
import com.opensurveys.dto.SendCodeRequest;
import com.opensurveys.dto.UpdateEmailRequest;
import com.opensurveys.dto.UpdatePasswordRequest;
import com.opensurveys.dto.UpdateProfileRequest;
import com.opensurveys.model.Form;
import com.opensurveys.model.User;
import com.opensurveys.model.VerificationCode;
import com.opensurveys.repository.FormRepository;
import com.opensurveys.repository.UserRepository;
import com.opensurveys.security.JwtUtil;
import com.opensurveys.service.VerificationCodeService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Self-service account settings for the signed-in user (JWT required).
 * Admin directory stays on UserController; this does not touch other users.
 */
@RestController
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "not authenticated"));
        }
        return ResponseEntity.ok(toProfile(user, null));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "not authenticated"));
        }

        String newUsername = request.getUsername() == null ? null : request.getUsername().trim();
        if (newUsername == null || newUsername.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username is required"));
        }

        Optional<User> sameName = userRepository.findByUsername(newUsername);
        if (sameName.isPresent() && !sameName.get().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "username already taken"));
        }

        boolean usernameChanged = !newUsername.equals(user.getUsername());
        user.setName(request.getName() == null ? "" : request.getName().trim());
        user.setUsername(newUsername);
        userRepository.save(user);

        String token = usernameChanged ? jwtUtil.generateToken(user.getUsername()) : null;
        return ResponseEntity.ok(toProfile(user, token));
    }

    @PutMapping("/email")
    public ResponseEntity<?> updateEmail(@RequestBody UpdateEmailRequest request) {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "not authenticated"));
        }

        String newEmail = request.getEmail() == null ? null : request.getEmail().trim();
        if (newEmail == null || newEmail.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "email is required"));
        }

        Optional<User> sameEmail = userRepository.findByEmailIgnoreCase(newEmail);
        if (sameEmail.isPresent() && !sameEmail.get().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "email already taken"));
        }

        ResponseEntity<?> authError = requirePasswordOrCode(
                user,
                request.getCurrentPassword(),
                request.getCode(),
                VerificationCode.PURPOSE_EMAIL_CHANGE
        );
        if (authError != null) {
            return authError;
        }

        user.setEmail(newEmail);
        userRepository.save(user);
        return ResponseEntity.ok(toProfile(user, null));
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody UpdatePasswordRequest request) {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "not authenticated"));
        }

        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "new password is required"));
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "password must be at least 6 characters"));
        }

        ResponseEntity<?> authError = requirePasswordOrCode(
                user,
                request.getCurrentPassword(),
                request.getCode(),
                VerificationCode.PURPOSE_PASSWORD_CHANGE
        );
        if (authError != null) {
            return authError;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(toProfile(user, null));
    }

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody SendCodeRequest request) {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "not authenticated"));
        }

        String purpose = normalizePurpose(request.getPurpose());
        if (purpose == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "purpose must be EMAIL_CHANGE or PASSWORD_CHANGE"));
        }

        try {
            verificationCodeService.sendCode(user, purpose);
            return ResponseEntity.ok(Map.of("message", "verification code sent"));
        } catch (VerificationCodeService.MailSendFailedException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<?> deleteAccount(@RequestBody DeleteAccountRequest request) {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "not authenticated"));
        }

        String confirm = request.getConfirmUsername() == null ? "" : request.getConfirmUsername().trim();
        if (!confirm.equals(user.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username confirmation does not match"));
        }

        if (User.ROLE_ADMIN.equals(user.getRole()) && countAdmins() <= 1) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "cannot delete the last admin"));
        }

        List<Form> forms = formRepository.findByCreatorOrderByIdDesc(user);
        formRepository.deleteAll(forms);
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> requirePasswordOrCode(
            User user,
            String currentPassword,
            String code,
            String purpose
    ) {
        boolean hasPasswordAttempt = currentPassword != null && !currentPassword.isBlank();
        boolean hasCodeAttempt = code != null && !code.isBlank();

        if (hasPasswordAttempt && hasCodeAttempt) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "provide either current password or verification code, not both"));
        }

        if (hasCodeAttempt) {
            if (!verificationCodeService.consumeCode(user, purpose, code)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid or expired verification code"));
            }
            return null;
        }

        if (hasPasswordAttempt) {
            if (user.getPassword() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "account has no password; use a verification code"));
            }
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "incorrect password"));
            }
            return null;
        }

        return ResponseEntity.badRequest()
                .body(Map.of("error", "current password or verification code is required"));
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }

    private AccountProfileResponse toProfile(User user, String token) {
        AccountProfileResponse response = new AccountProfileResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getPassword() != null && !user.getPassword().isBlank()
        );
        response.setToken(token);
        return response;
    }

    private long countAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> User.ROLE_ADMIN.equals(u.getRole()))
                .count();
    }

    private static String normalizePurpose(String purpose) {
        if (purpose == null) {
            return null;
        }
        String normalized = purpose.trim().toUpperCase();
        if (VerificationCode.PURPOSE_EMAIL_CHANGE.equals(normalized)
                || VerificationCode.PURPOSE_PASSWORD_CHANGE.equals(normalized)) {
            return normalized;
        }
        return null;
    }
}
