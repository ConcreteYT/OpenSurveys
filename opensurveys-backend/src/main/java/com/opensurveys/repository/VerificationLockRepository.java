package com.opensurveys.repository;

import com.opensurveys.model.VerificationLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationLockRepository extends JpaRepository<VerificationLock, Long> {

    Optional<VerificationLock> findByUserIdAndPurpose(Long userId, String purpose);

    void deleteByUserId(Long userId);
}
