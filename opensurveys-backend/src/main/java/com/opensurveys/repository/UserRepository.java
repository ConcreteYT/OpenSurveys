package com.opensurveys.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opensurveys.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    // Used by:
    //  - AuthController#register to reject duplicate usernames, and #login to fetch
    //    the account to verify the password against.
    //  - JwtAuthFilter to resolve the username embedded in a validated JWT back into
    //    a User before populating the SecurityContext.
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmailIgnoreCase(String email);

    // Roles are always written uppercase (User.ROLE_USER / User.ROLE_ADMIN).
    long countByRole(String role);

    default boolean isUsernameTakenByOther(String username, Long userId) {
        return findByUsername(username)
                .map(existing -> !existing.getId().equals(userId))
                .orElse(false);
    }
}
