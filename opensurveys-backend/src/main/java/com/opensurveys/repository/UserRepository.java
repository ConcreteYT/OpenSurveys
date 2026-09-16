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
    //  - FormController#createForm to resolve the authenticated principal's username
    //    into the User that becomes the new Form's creator.
    Optional<User> findByUsername(String username);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmailIgnoreCase(String email);
}
