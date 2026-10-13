CREATE TABLE IF NOT EXISTS `USER` (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NULL,
    name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    password VARCHAR(255) NULL,
    google_id VARCHAR(255) NULL,
    role VARCHAR(255) NULL,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS FORM (
    form_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NULL,
    access_code VARCHAR(8) NULL,
    responses_public BIT(1) NOT NULL DEFAULT 1,
    one_response_only BIT(1) NOT NULL DEFAULT 1,
    USER_user_id BIGINT NULL,
    PRIMARY KEY (form_id),
    UNIQUE KEY uk_form_access_code (access_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS QUESTION (
    question_id BIGINT NOT NULL AUTO_INCREMENT,
    questiontext VARCHAR(255) NULL,
    questiontype INT NULL,
    questionoptions VARCHAR(255) NULL,
    FORM_form_id BIGINT NULL,
    PRIMARY KEY (question_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ANSWER (
    answer_id BIGINT NOT NULL AUTO_INCREMENT,
    answer TEXT NULL,
    QUESTION_question_id BIGINT NULL,
    PRIMARY KEY (answer_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS VERIFICATION_CODE (
    code_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used BIT(1) NOT NULL,
    PRIMARY KEY (code_id)
) ENGINE=InnoDB;
