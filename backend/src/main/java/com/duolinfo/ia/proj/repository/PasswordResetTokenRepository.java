package com.duolinfo.ia.proj.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.duolinfo.ia.proj.entity.PasswordResetToken;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    List<PasswordResetToken> findByUser_IdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<PasswordResetToken> findByUser_EmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc(String email);

    Optional<PasswordResetToken> findFirstByUser_IdAndTokenHashAndUsedAtIsNullOrderByCreatedAtDesc(
        Long userId,
        String tokenHash
    );

    @Modifying
    @Query("update PasswordResetToken token set token.usedAt = :usedAt "
        + "where token.id = :tokenId and token.usedAt is null and token.expiresAt >= :usedAt")
    int markUsedIfUnusedAndNotExpired(@Param("tokenId") Long tokenId, @Param("usedAt") Instant usedAt);

    long countByUser_IdAndCreatedAtAfter(Long userId, Instant since);

    long countByUser_EmailIgnoreCaseAndCreatedAtAfter(String email, Instant since);

    void deleteByUser_Id(Long userId);
}
