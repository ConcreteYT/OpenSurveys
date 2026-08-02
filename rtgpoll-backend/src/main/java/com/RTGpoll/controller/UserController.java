package com.RTGpoll.controller;

import com.RTGpoll.dto.UpdateUserRequest;
import com.RTGpoll.model.Form;
import com.RTGpoll.model.User;
import com.RTGpoll.repository.FormRepository;
import com.RTGpoll.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// Admin account directory. Listing/updating/deleting users is locked to ROLE_ADMIN
// in SecurityConfig. Account creation still lives exclusively in AuthController#signup.
@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FormRepository formRepository;

    // Password is never included here - User.password is annotated @JsonIgnore.
    @GetMapping("/users")
    List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "user not found"));
        }

        User user = userOpt.get();
        String newRole = normalizeRole(request.getRole());
        if (newRole == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "role must be USER or ADMIN"));
        }

        String newUsername = request.getUsername() == null ? null : request.getUsername().trim();
        if (newUsername == null || newUsername.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username is required"));
        }

        Optional<User> sameName = userRepository.findByUsername(newUsername);
        if (sameName.isPresent() && !sameName.get().getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "username already taken"));
        }

        // Refuse demoting/removing the last remaining admin.
        if (User.ROLE_ADMIN.equals(user.getRole()) && User.ROLE_USER.equals(newRole)
                && countAdmins() <= 1) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "cannot demote the last admin"));
        }

        user.setName(request.getName() == null ? "" : request.getName().trim());
        user.setUsername(newUsername);
        user.setEmail(request.getEmail() == null ? "" : request.getEmail().trim());
        user.setRole(newRole);
        userRepository.save(user);

        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "user not found"));
        }

        User user = userOpt.get();
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        if (user.getUsername().equals(currentUsername)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "cannot delete your own account"));
        }

        if (User.ROLE_ADMIN.equals(user.getRole()) && countAdmins() <= 1) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "cannot delete the last admin"));
        }

        // Forms reference USER via FK; remove owned surveys (and cascaded questions/answers) first.
        List<Form> forms = formRepository.findByCreatorOrderByIdDesc(user);
        formRepository.deleteAll(forms);
        userRepository.delete(user);

        return ResponseEntity.noContent().build();
    }

    private long countAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> User.ROLE_ADMIN.equals(u.getRole()))
                .count();
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String normalized = role.trim().toUpperCase();
        if (User.ROLE_USER.equals(normalized) || User.ROLE_ADMIN.equals(normalized)) {
            return normalized;
        }
        return null;
    }
}
