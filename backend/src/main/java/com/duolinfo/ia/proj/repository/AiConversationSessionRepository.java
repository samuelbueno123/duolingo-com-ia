package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.AiConversationSession;

@Repository
public interface AiConversationSessionRepository extends JpaRepository<AiConversationSession, Long> {
    List<AiConversationSession> findByStudentIdOrderByStartedAtDesc(Long studentId);
    List<AiConversationSession> findByStudentIdAndTargetLanguageOrderByStartedAtDesc(Long studentId, String targetLanguage);
}
