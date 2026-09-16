package com.opensurveys.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opensurveys.model.User;
import com.opensurveys.model.VerificationCode;
import com.opensurveys.repository.VerificationCodeRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Issues and validates 6-digit email verification codes for sensitive account changes.
 * Codes are stored as BCrypt hashes with a 10-minute TTL.
 */
@Service
public class VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeService.class);
    private static final int CODE_TTL_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Transactional
    public void sendCode(User user, String purpose) throws MailSendFailedException {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new MailSendFailedException("account has no email address");
        }

        verificationCodeRepository.invalidateUnused(user.getId(), purpose);

        String plainCode = String.format("%06d", RANDOM.nextInt(1_000_000));
        VerificationCode record = new VerificationCode();
        record.setUserId(user.getId());
        record.setPurpose(purpose);
        record.setCodeHash(passwordEncoder.encode(plainCode));
        record.setExpiresAt(Instant.now().plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES));
        record.setUsed(false);
        verificationCodeRepository.save(record);

        log.debug("Verification code for user {} purpose {}: {}", user.getId(), purpose, plainCode);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(user.getEmail());
            message.setSubject("OpenSurveys verification code");
            message.setText(
                    "Your OpenSurveys verification code is: " + plainCode + "\n\n"
                            + "It expires in " + CODE_TTL_MINUTES + " minutes.\n"
                            + "If you did not request this, you can ignore this email."
            );
            mailSender.send(message);
        } catch (MailException | IllegalStateException e) {
            log.warn("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
            throw new MailSendFailedException("Failed to send verification email");
        }
    }

    @Transactional
    public boolean consumeCode(User user, String purpose, String plainCode) {
        if (plainCode == null || plainCode.isBlank()) {
            return false;
        }

        List<VerificationCode> candidates =
                verificationCodeRepository.findByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
                        user.getId(), purpose);
        Instant now = Instant.now();

        for (VerificationCode candidate : candidates) {
            if (candidate.getExpiresAt().isBefore(now)) {
                candidate.setUsed(true);
                verificationCodeRepository.save(candidate);
                continue;
            }
            if (passwordEncoder.matches(plainCode.trim(), candidate.getCodeHash())) {
                candidate.setUsed(true);
                verificationCodeRepository.save(candidate);
                return true;
            }
        }
        return false;
    }

    public static class MailSendFailedException extends Exception {
        public MailSendFailedException(String message) {
            super(message);
        }
    }
}
