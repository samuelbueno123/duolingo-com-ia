package com.duolinfo.ia.proj.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.duolinfo.ia.proj.entity.PasswordResetToken;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    List<PasswordResetToken> findByUser_IdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<PasswordResetToken> findByUser_EmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc(String email);

    Optional<PasswordResetToken> findFirstByUser_IdAndTokenHashAndUsedAtIsNullOrderByCreatedAtDesc(
        Long userId,
        String tokenHash
    );

    long countByUser_IdAndCreatedAtAfter(Long userId, Instant since);

    long countByUser_EmailIgnoreCaseAndCreatedAtAfter(String email, Instant since);

    void deleteByUser_Id(Long userId);
}
