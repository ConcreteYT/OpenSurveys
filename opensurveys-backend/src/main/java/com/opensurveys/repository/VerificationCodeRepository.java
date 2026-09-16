package com.opensurveys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.opensurveys.model.VerificationCode;

import java.util.List;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    List<VerificationCode> findByUserIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(Long userId, String purpose);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE VerificationCode v SET v.used = true WHERE v.userId = :userId AND v.purpose = :purpose AND v.used = false")
    void invalidateUnused(@Param("userId") Long userId, @Param("purpose") String purpose);
}
