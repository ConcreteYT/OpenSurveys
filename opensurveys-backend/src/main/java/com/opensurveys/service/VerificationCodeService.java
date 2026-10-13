package com.opensurveys.service;

import com.opensurveys.model.User;
import com.opensurveys.model.VerificationCode;
import com.opensurveys.model.VerificationLock;
import com.opensurveys.repository.VerificationCodeRepository;
import com.opensurveys.repository.VerificationLockRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

/**
 * Issues and checks 8-digit email codes. Codes are stored as HMAC-SHA256, never logged.
 * Five wrong checks lock that purpose for 24 hours.
 */
@Service
public class VerificationCodeService {

    public static final int MAX_ATTEMPTS = 5;
    public static final int LOCKOUT_HOURS = 24;

    private static final int CODE_TTL_MINUTES = 10;
    private static final int CODE_BOUND = 100_000_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private VerificationLockRepository verificationLockRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${jwt.secret}")
    private String pepper;

    @Transactional
    public void sendCode(User user, String purpose) throws MailSendFailedException, LockedOutException {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new MailSendFailedException("account has no email address");
        }
        VerificationLock lock = lockFor(user.getId(), purpose);
        if (isLocked(lock)) {
            throw new LockedOutException();
        }

        verificationCodeRepository.invalidateUnused(user.getId(), purpose);

        String plainCode = String.format("%08d", RANDOM.nextInt(CODE_BOUND));
        VerificationCode record = new VerificationCode();
        record.setUserId(user.getId());
        record.setPurpose(purpose);
        record.setCodeHash(hashCode(plainCode));
        record.setExpiresAt(Instant.now().plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES));
        record.setUsed(false);
        verificationCodeRepository.save(record);

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
            throw new MailSendFailedException("Failed to send verification email");
        }
    }

    @Transactional
    public ConsumeResult consumeCode(User user, String purpose, String plainCode) {
        if (plainCode == null || plainCode.isBlank()) {
            return ConsumeResult.INVALID;
        }
        VerificationLock lock = lockFor(user.getId(), purpose);
        if (isLocked(lock)) {
            return ConsumeResult.LOCKED;
        }

        List<VerificationCode> candidates =
                verificationCodeRepository.findByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
                        user.getId(), purpose);
        Instant now = Instant.now();
        String trimmedCode = plainCode.trim();

        for (VerificationCode candidate : candidates) {
            if (candidate.getExpiresAt().isBefore(now)) {
                candidate.setUsed(true);
                continue;
            }
            if (matches(trimmedCode, candidate.getCodeHash())) {
                candidate.setUsed(true);
                lock.setFailedAttempts(0);
                lock.setLockedUntil(null);
                return ConsumeResult.OK;
            }
        }

        int attempts = lock.getFailedAttempts() + 1;
        lock.setFailedAttempts(attempts);
        if (attempts >= MAX_ATTEMPTS) {
            lock.setLockedUntil(now.plus(LOCKOUT_HOURS, ChronoUnit.HOURS));
            return ConsumeResult.LOCKED;
        }
        return ConsumeResult.INVALID;
    }

    private VerificationLock lockFor(Long userId, String purpose) {
        return verificationLockRepository.findByUserIdAndPurpose(userId, purpose)
                .orElseGet(() -> {
                    VerificationLock created = new VerificationLock();
                    created.setUserId(userId);
                    created.setPurpose(purpose);
                    created.setFailedAttempts(0);
                    try {
                        return verificationLockRepository.saveAndFlush(created);
                    } catch (DataIntegrityViolationException ex) {
                        return verificationLockRepository.findByUserIdAndPurpose(userId, purpose).orElseThrow();
                    }
                });
    }

    private static boolean isLocked(VerificationLock lock) {
        return lock.getLockedUntil() != null && lock.getLockedUntil().isAfter(Instant.now());
    }

    private String hashCode(String plain) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "h:" + HexFormat.of().formatHex(mac.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not hash verification code", e);
        }
    }

    private boolean matches(String plain, String stored) {
        if (stored != null && stored.startsWith("h:")) {
            byte[] expected = hashCode(plain).getBytes(StandardCharsets.UTF_8);
            byte[] actual = stored.getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expected, actual);
        }
        return stored != null && passwordEncoder.matches(plain, stored);
    }

    public enum ConsumeResult {
        OK,
        INVALID,
        LOCKED
    }

    public static class MailSendFailedException extends Exception {
        public MailSendFailedException(String message) {
            super(message);
        }
    }

    public static class LockedOutException extends Exception {
        public LockedOutException() {
            super("too many attempts; try again in 24 hours");
        }
    }
}
