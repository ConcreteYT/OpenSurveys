package com.opensurveys.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.opensurveys.model.User;
import com.opensurveys.repository.UserRepository;

/**
 * Ensures a testable admin account exists on startup.
 * Credentials come from app.admin.username / app.admin.password in application.properties.
 * If the username already exists, its role is promoted to ADMIN (password is left unchanged
 * unless the account was just created).
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        userRepository.findByUsername(adminUsername).ifPresentOrElse(existing -> {
            boolean changed = false;
            if (!User.ROLE_ADMIN.equals(existing.getRole())) {
                existing.setRole(User.ROLE_ADMIN);
                changed = true;
            }
            // Keep password aligned with app.admin.password so local testing stays predictable.
            if (!passwordEncoder.matches(adminPassword, existing.getPassword())) {
                existing.setPassword(passwordEncoder.encode(adminPassword));
                changed = true;
            }
            if (changed) {
                userRepository.save(existing);
                log.info("Updated ADMIN account '{}'", adminUsername);
            }
        }, () -> {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setName("Admin");
            admin.setEmail(adminUsername + "@opensurveys.local");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(User.ROLE_ADMIN);
            userRepository.save(admin);
            log.info("Created ADMIN account '{}' (see app.admin.password)", adminUsername);
        });
    }
}
