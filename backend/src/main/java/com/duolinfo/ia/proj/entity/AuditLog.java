package com.duolinfo.ia.proj.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp DESC"),
        @Index(name = "idx_audit_logs_category", columnList = "category"),
        @Index(name = "idx_audit_logs_severity", columnList = "severity"),
        @Index(name = "idx_audit_logs_actor", columnList = "actor_email"),
        @Index(name = "idx_audit_logs_result", columnList = "result"),
        @Index(name = "idx_audit_logs_action", columnList = "action")
    }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 50)
    private String category; // AUTH, USER, ADMIN, SECURITY, CATALOG, DOCUMENT, SYSTEM

    @Column(nullable = false, length = 20)
    private String severity; // INFO, WARN, ERROR

    @Column(nullable = false, length = 100)
    private String action; // e.g. LOGIN_SUCCESS, LOGIN_FAILURE, USER_UPDATED, ROLE_GRANTED, etc.

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(name = "target_entity", length = 100)
    private String targetEntity;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(nullable = false, length = 30)
    private String result; // SUCCESS, FAILURE, ERROR

    @Column(name = "client_ip", length = 60)
    private String clientIp;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(columnDefinition = "TEXT")
    private String details;
}
