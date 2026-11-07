package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.PronunciationChallenge;

@Repository
public interface PronunciationChallengeRepository extends JpaRepository<PronunciationChallenge, Long> {
    List<PronunciationChallenge> findByLanguageIgnoreCase(String language);
    List<PronunciationChallenge> findByLanguageIgnoreCaseAndCefrLevelIgnoreCase(String language, String cefrLevel);
}
