package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.dto.response.AuditLogPageResponseDTO;
import com.duolinfo.ia.proj.dto.response.AuditLogResponseDTO;
import com.duolinfo.ia.proj.entity.AuditLog;
import com.duolinfo.ia.proj.repository.AuditLogRepository;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Predicate;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog recordEvent(
            String category,
            String severity,
            String action,
            String actorEmail,
            String actorRole,
            String targetEntity,
            String targetId,
            String result,
            String clientIp,
            String correlationId,
            String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .category(category != null ? category.toUpperCase() : "SYSTEM")
                .severity(severity != null ? severity.toUpperCase() : "INFO")
                .action(action != null ? action : "UNKNOWN_ACTION")
                .actorEmail(actorEmail != null ? actorEmail : "anonymous")
                .actorRole(actorRole != null ? actorRole.toUpperCase() : "ANONYMOUS")
                .targetEntity(targetEntity)
                .targetId(targetId)
                .result(result != null ? result.toUpperCase() : "SUCCESS")
                .clientIp(clientIp)
                .correlationId(correlationId)
                .details(details)
                .build();

            AuditLog saved = auditLogRepository.save(auditLog);
            log.info("[AUDIT-RECORDED] id={} action={} actor={} severity={} result={}",
                saved.getId(), saved.getAction(), saved.getActorEmail(), saved.getSeverity(), saved.getResult());
            return saved;
        } catch (Exception e) {
            log.error("[AUDIT-FAILED] Erro ao gravar registro de auditoria: {}", e.getMessage(), e);
            return null;
        }
    }

    @Transactional(readOnly = true)
    public AuditLogPageResponseDTO searchLogs(
            String category,
            String severity,
            String result,
            String actorEmail,
            String search,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "timestamp"));

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
                predicates.add(cb.equal(cb.upper(root.get("category")), category.trim().toUpperCase()));
            }

            if (severity != null && !severity.isBlank() && !"ALL".equalsIgnoreCase(severity)) {
                predicates.add(cb.equal(cb.upper(root.get("severity")), severity.trim().toUpperCase()));
            }

            if (result != null && !result.isBlank() && !"ALL".equalsIgnoreCase(result)) {
                predicates.add(cb.equal(cb.upper(root.get("result")), result.trim().toUpperCase()));
            }

            if (actorEmail != null && !actorEmail.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("actorEmail")), "%" + actorEmail.trim().toLowerCase() + "%"));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endDate));
            }

            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate pAction = cb.like(cb.lower(root.get("action")), term);
                Predicate pDetails = cb.like(cb.lower(root.get("details")), term);
                Predicate pTarget = cb.like(cb.lower(root.get("targetEntity")), term);
                Predicate pActor = cb.like(cb.lower(root.get("actorEmail")), term);
                predicates.add(cb.or(pAction, pDetails, pTarget, pActor));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditLog> pageResult = auditLogRepository.findAll(spec, pageable);
        List<AuditLogResponseDTO> dtoList = pageResult.getContent().stream()
            .map(this::toDto)
            .toList();

        return new AuditLogPageResponseDTO(
            dtoList,
            pageResult.getTotalElements(),
            pageResult.getTotalPages(),
            pageResult.getNumber(),
            pageResult.getSize()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getSeverityCounts() {
        return Map.of(
            "total", auditLogRepository.count(),
            "infoCount", auditLogRepository.countBySeverity("INFO"),
            "warnCount", auditLogRepository.countBySeverity("WARN"),
            "errorCount", auditLogRepository.countBySeverity("ERROR")
        );
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponseDTO> getRecentLogs(int limit) {
        return auditLogRepository.findTop10ByOrderByTimestampDesc().stream()
            .map(this::toDto)
            .toList();
    }

    public AuditLogResponseDTO toDto(AuditLog log) {
        return new AuditLogResponseDTO(
            log.getId(),
            log.getTimestamp(),
            log.getCategory(),
            log.getSeverity(),
            log.getAction(),
            log.getActorEmail(),
            log.getActorRole(),
            log.getTargetEntity(),
            log.getTargetId(),
            log.getResult(),
            log.getClientIp(),
            log.getCorrelationId(),
            log.getDetails()
        );
    }

    @PostConstruct
    public void initBaselineLogs() {
        if (auditLogRepository.count() == 0) {
            recordEvent(
                "SYSTEM",
                "INFO",
                "SYSTEM_INITIALIZATION",
                "system@duolinfo.com",
                "SYSTEM",
                "Application",
                "1",
                "SUCCESS",
                "127.0.0.1",
                "boot-00001",
                "Sistema Duolinfo inicializado com sucesso. Subsistemas de segurança, cache e auditoria ativos."
            );
            recordEvent(
                "AUTH",
                "INFO",
                "ADMIN_SEEDED",
                "admin@duolingo.com",
                "ADMIN",
                "User",
                "1",
                "SUCCESS",
                "127.0.0.1",
                "boot-00002",
                "Conta SuperAdmin padrão verificada e provisionada com perfil SUPER_ADMIN."
            );
            recordEvent(
                "CATALOG",
                "INFO",
                "CATALOG_INITIALIZED",
                "system@duolinfo.com",
                "SYSTEM",
                "CatalogOption",
                "ALL",
                "SUCCESS",
                "127.0.0.1",
                "boot-00003",
                "Catálogo de idiomas e níveis de proficiência carregado e indexado no Spring Cache."
            );
        }
    }
}
