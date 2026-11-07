package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.PronunciationAttempt;

@Repository
public interface PronunciationAttemptRepository extends JpaRepository<PronunciationAttempt, Long> {
    List<PronunciationAttempt> findByStudentIdOrderByAttemptedAtDesc(Long studentId);
    List<PronunciationAttempt> findByStudentIdAndChallengeIdOrderByAttemptedAtDesc(Long studentId, Long challengeId);
}
