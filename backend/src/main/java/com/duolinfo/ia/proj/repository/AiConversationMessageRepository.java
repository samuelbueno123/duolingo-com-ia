package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.AiConversationMessage;

@Repository
public interface AiConversationMessageRepository extends JpaRepository<AiConversationMessage, Long> {
    List<AiConversationMessage> findBySessionIdOrderByTimestampAsc(Long sessionId);

    @Query("SELECT m FROM AiConversationMessage m WHERE m.sessionId IN (SELECT s.id FROM AiConversationSession s WHERE s.studentId = :studentId) AND m.hasGrammarIssues = true ORDER BY m.timestamp DESC")
    List<AiConversationMessage> findGrammarIssuesByStudentId(@Param("studentId") Long studentId);
}
