package com.opensurveys.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import com.opensurveys.model.User;
import com.opensurveys.model.VerificationCode;
import com.opensurveys.repository.UserRepository;
import com.opensurveys.security.AuthCookieService;
import com.opensurveys.security.ClientRateLimiter;
import com.opensurveys.service.CurrentUserService;
import com.opensurveys.service.PasswordPolicy;
import com.opensurveys.service.UserDeletionService;
import com.opensurveys.service.VerificationCodeService;
import com.opensurveys.service.VerificationCodeService.ConsumeResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
    private CurrentUserService currentUserService;

    @Autowired
    private UserDeletionService userDeletionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthCookieService authCookieService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private ClientRateLimiter rateLimiter;

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "not authenticated"));
        }
        return ResponseEntity.ok(toProfile(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request, HttpServletResponse httpResponse) {
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

        if (userRepository.isUsernameTakenByOther(newUsername, user.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "username already taken"));
        }

        boolean usernameChanged = !newUsername.equals(user.getUsername());
        user.setName(request.getName() == null ? "" : request.getName().trim());
        user.setUsername(newUsername);
        userRepository.save(user);

        if (usernameChanged) {
            authCookieService.write(httpResponse, user);
        }
        return ResponseEntity.ok(toProfile(user));
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
        return ResponseEntity.ok(toProfile(user));
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody UpdatePasswordRequest request, HttpServletResponse httpResponse) {
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
        if (PasswordPolicy.isTooShort(newPassword)) {
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
        user.bumpTokenVersion();
        userRepository.save(user);
        authCookieService.write(httpResponse, user);
        return ResponseEntity.ok(toProfile(user));
    }

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody SendCodeRequest request, HttpServletRequest httpRequest) {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "not authenticated"));
        }

        if (!rateLimiter.allow("send-code:" + httpRequest.getRemoteAddr(), 5, 10 * 60 * 1000L)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", "too many requests"));
        }

        String purpose = normalizePurpose(request.getPurpose());
        if (purpose == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "purpose must be EMAIL_CHANGE, PASSWORD_CHANGE, or ACCOUNT_DELETE"));
        }

        try {
            verificationCodeService.sendCode(user, purpose);
            return ResponseEntity.ok(Map.of("message", "verification code sent"));
        } catch (VerificationCodeService.MailSendFailedException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (VerificationCodeService.LockedOutException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAccount(@RequestBody DeleteAccountRequest request, HttpServletResponse httpResponse) {
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

        if (User.ROLE_ADMIN.equals(user.getRole()) && userRepository.countByRole(User.ROLE_ADMIN) <= 1) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "cannot delete the last admin"));
        }

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            if (request.getCurrentPassword() == null
                    || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "incorrect password"));
            }
        } else {
            ResponseEntity<?> authError = requirePasswordOrCode(
                    user,
                    null,
                    request.getCode(),
                    VerificationCode.PURPOSE_ACCOUNT_DELETE
            );
            if (authError != null) {
                return authError;
            }
        }

        user.bumpTokenVersion();
        userRepository.save(user);
        userDeletionService.deleteUserAndForms(user);
        authCookieService.clear(httpResponse);
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
            ConsumeResult result = verificationCodeService.consumeCode(user, purpose, code);
            if (result == ConsumeResult.LOCKED) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "too many attempts; try again in 24 hours"));
            }
            if (result != ConsumeResult.OK) {
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
        return currentUserService.get().orElse(null);
    }

    private AccountProfileResponse toProfile(User user) {
        return new AccountProfileResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getPassword() != null && !user.getPassword().isBlank()
        );
    }

    private static String normalizePurpose(String purpose) {
        if (purpose == null) {
            return null;
        }
        String normalized = purpose.trim().toUpperCase();
        if (VerificationCode.PURPOSE_EMAIL_CHANGE.equals(normalized)
                || VerificationCode.PURPOSE_PASSWORD_CHANGE.equals(normalized)
                || VerificationCode.PURPOSE_ACCOUNT_DELETE.equals(normalized)) {
            return normalized;
        }
        return null;
    }
}
