package com.opensurveys.repository;

import com.opensurveys.model.RespondentToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface RespondentTokenRepository extends JpaRepository<RespondentToken, String> {

    @Modifying
    @Query("update RespondentToken t set t.used = true where t.tokenHash = :hash and t.formId = :formId and t.used = false and t.expiresAt > :now")
    int markUsed(@Param("hash") String hash, @Param("formId") Long formId, @Param("now") Instant now);

    @Modifying
    @Query("update RespondentToken t set t.used = false where t.tokenHash = :hash and t.formId = :formId")
    int markUnused(@Param("hash") String hash, @Param("formId") Long formId);

    @Modifying
    @Query("delete from RespondentToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
