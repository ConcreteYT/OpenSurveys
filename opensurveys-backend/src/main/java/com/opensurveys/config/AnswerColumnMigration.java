package com.opensurveys.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate ddl-auto=update often does not widen an existing ANSWER.answer column to TEXT.
 * Image upload commits store semicolon-separated filenames and need a wide column.
 */
@Component
public class AnswerColumnMigration {

    private static final Logger log = LoggerFactory.getLogger(AnswerColumnMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public AnswerColumnMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void widenAnswerColumn() {
        try {
            jdbcTemplate.execute("ALTER TABLE `ANSWER` MODIFY COLUMN `answer` TEXT");
            log.info("Ensured ANSWER.answer column is TEXT");
        } catch (Exception e) {
            log.warn("Could not alter ANSWER.answer to TEXT: {}", e.getMessage());
        }
    }
}
