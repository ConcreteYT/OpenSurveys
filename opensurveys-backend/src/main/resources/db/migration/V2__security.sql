ALTER TABLE `USER`
    ADD COLUMN email_verified BIT(1) NOT NULL DEFAULT 1,
    ADD COLUMN token_version INT NOT NULL DEFAULT 0;

ALTER TABLE `USER` ADD CONSTRAINT uk_user_username UNIQUE (username);
ALTER TABLE `USER` ADD CONSTRAINT uk_user_email UNIQUE (email);

CREATE INDEX idx_vcode_user_purpose ON VERIFICATION_CODE (user_id, purpose, used);

CREATE TABLE VERIFICATION_LOCK (
    lock_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME(6) NULL,
    PRIMARY KEY (lock_id),
    UNIQUE KEY uk_lock_user_purpose (user_id, purpose)
) ENGINE=InnoDB;

CREATE TABLE RESPONDENT_TOKEN (
    token_hash CHAR(64) NOT NULL,
    form_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used BIT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (token_hash),
    KEY idx_resp_form_expiry (form_id, expires_at),
    CONSTRAINT fk_resp_form FOREIGN KEY (form_id) REFERENCES FORM (form_id) ON DELETE CASCADE
) ENGINE=InnoDB;
