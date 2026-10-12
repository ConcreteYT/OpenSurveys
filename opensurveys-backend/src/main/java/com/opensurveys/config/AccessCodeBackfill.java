package com.opensurveys.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.opensurveys.service.AccessCodes;

import java.security.SecureRandom;
import java.util.List;

/**
 * Assigns a unique 8-digit access code to every FORM row whose access_code is still null.
 * The column stays nullable in the entity so Hibernate can add it before this runs.
 */
@Component
public class AccessCodeBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccessCodeBackfill.class);

    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccessCodeBackfill(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Long> ids = jdbcTemplate.query(
                "SELECT form_id FROM FORM WHERE access_code IS NULL",
                (rs, rowNum) -> rs.getLong("form_id"));
        for (Long id : ids) {
            assign(id);
        }
        if (!ids.isEmpty()) {
            log.info("Assigned access codes to {} existing form(s)", ids.size());
        }
    }

    private void assign(long formId) {
        for (int attempt = 0; attempt < AccessCodes.MAX_ATTEMPTS; attempt++) {
            String code = AccessCodes.generate(secureRandom);
            Long taken = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM FORM WHERE access_code = ?",
                    Long.class,
                    code);
            if (taken != null && taken > 0) {
                continue;
            }
            try {
                int updated = jdbcTemplate.update(
                        "UPDATE FORM SET access_code = ? WHERE form_id = ? AND access_code IS NULL",
                        code,
                        formId);
                if (updated == 1 || alreadyAssigned(formId)) {
                    return;
                }
            } catch (DataIntegrityViolationException ex) {
                // unique collision; try another code
            }
        }
        throw new IllegalStateException("Could not allocate a unique access code for form " + formId);
    }

    private boolean alreadyAssigned(long formId) {
        String existing = jdbcTemplate.query(
                "SELECT access_code FROM FORM WHERE form_id = ?",
                rs -> rs.next() ? rs.getString("access_code") : null,
                formId);
        return existing != null;
    }
}
