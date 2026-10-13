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
 * Creates the configured admin account once. An existing username is left unchanged.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByUsername(adminUsername).isPresent()) {
            log.info("Username '{}' already exists; admin bootstrap left it unchanged", adminUsername);
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("APP_ADMIN_PASSWORD is empty, so no admin account was created");
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setName("Admin");
        admin.setEmail(adminUsername + "@opensurveys.local");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(User.ROLE_ADMIN);
        admin.setEmailVerified(true);
        userRepository.save(admin);
        log.info("Created ADMIN account '{}'", adminUsername);
    }
}
