package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.InstitutionRequestCreateDTO;
import com.duolinfo.ia.proj.dto.request.InstitutionRequestDecisionDTO;
import com.duolinfo.ia.proj.dto.request.TeacherRequestDecisionDTO;
import com.duolinfo.ia.proj.dto.response.AdminNotificationResponseDTO;
import com.duolinfo.ia.proj.dto.response.InstitutionRequestResponseDTO;
import com.duolinfo.ia.proj.dto.response.TeacherResponseDTO;
import com.duolinfo.ia.proj.entity.AdminNotification;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.InstitutionRequest;
import com.duolinfo.ia.proj.entity.InstitutionRequest.Status;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.repository.AdminNotificationRepository;
import com.duolinfo.ia.proj.repository.InstitutionRequestRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service

public class InstitutionRequestService {

    private static final String REQUEST_REFERENCE = "INSTITUTION_REQUEST";
    private static final String TEACHER_REFERENCE = TeacherService.TEACHER_REQUEST_REFERENCE;
    private static final int EXPIRATION_DAYS = 7;

    private final InstitutionRequestRepository requestRepository;
    private final AdminNotificationRepository notificationRepository;
    private final InstitutionService institutionService;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final SseEmitterService sseEmitterService;
    private final com.duolinfo.ia.proj.config.UserAuthCache userAuthCache;

    public InstitutionRequestService(
            InstitutionRequestRepository requestRepository,
            AdminNotificationRepository notificationRepository,
            InstitutionService institutionService,
            TeacherRepository teacherRepository,
            UserRepository userRepository) {
        this(requestRepository, notificationRepository, institutionService, teacherRepository, userRepository, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public InstitutionRequestService(
            InstitutionRequestRepository requestRepository,
            AdminNotificationRepository notificationRepository,
            InstitutionService institutionService,
            TeacherRepository teacherRepository,
            UserRepository userRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) SseEmitterService sseEmitterService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.duolinfo.ia.proj.config.UserAuthCache userAuthCache) {
        this.requestRepository = requestRepository;
        this.notificationRepository = notificationRepository;
        this.institutionService = institutionService;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.sseEmitterService = sseEmitterService;
        this.userAuthCache = userAuthCache;
    }

    @Transactional
    public InstitutionRequestResponseDTO create(InstitutionRequestCreateDTO request) {
        InstitutionRequest institutionRequest = new InstitutionRequest();
        institutionRequest.setRequestedName(request.requestedName().trim());
        institutionRequest.setRequesterName(request.requesterName().trim());
        institutionRequest.setRequesterEmail(request.requesterEmail().trim().toLowerCase(Locale.ROOT));
        institutionRequest.setCity(clean(request.city()));
        institutionRequest.setState(clean(request.state()));
        institutionRequest.setMessage(clean(request.message()));
        institutionRequest.setStatus(Status.PENDING);
        InstitutionRequest savedRequest = requestRepository.save(institutionRequest);

        AdminNotification notification = new AdminNotification();
        notification.setType(REQUEST_REFERENCE);
        notification.setTitle("Solicitação de instituição");
        notification.setMessage(savedRequest.getRequesterName()
            + " solicitou o cadastro de " + savedRequest.getRequestedName() + ".");
        notification.setReferenceType(REQUEST_REFERENCE);
        notification.setReferenceId(savedRequest.getId());
        AdminNotification savedNotification = notificationRepository.save(notification);
        notifyAdminNotification(savedNotification);

        return toRequestResponse(savedRequest);
    }

    @Transactional
    public List<AdminNotificationResponseDTO> notifications(String adminEmail) {
        expireAndSyncRequests();
        Long scopedInstitutionId = resolveScopedInstitutionId(adminEmail);
        List<AdminNotification> list = scopedInstitutionId == null
            ? notificationRepository.findAllByOrderByCreatedAtDesc()
            : notificationRepository.findByInstitutionIdOrderByCreatedAtDesc(scopedInstitutionId);
        return list.stream()
            .map(this::toNotificationResponse)
            .toList();
    }

    @Transactional
    public long unreadCount(String adminEmail) {
        expireAndSyncRequests();
        Long scopedInstitutionId = resolveScopedInstitutionId(adminEmail);
        return scopedInstitutionId == null
            ? notificationRepository.countByReadFalse()
            : notificationRepository.countByInstitutionIdAndReadFalse(scopedInstitutionId);
    }

    @Transactional
    public AdminNotificationResponseDTO markRead(Long notificationId) {
        AdminNotification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Notificação não encontrada."));
        notification.setRead(true);
        return toNotificationResponse(notificationRepository.save(notification));
    }

    @Transactional
    public List<InstitutionRequestResponseDTO> requests(Status status) {
        expireAndSyncRequests();
        List<InstitutionRequest> requests = status == null
            ? requestRepository.findAllByOrderByCreatedAtDesc()
            : requestRepository.findByStatusOrderByCreatedAtDesc(status);
        return requests.stream().map(this::toRequestResponse).toList();
    }

    @Transactional
    public InstitutionRequestResponseDTO decide(
            Long requestId,
            InstitutionRequestDecisionDTO decision) {
        expireAndSyncRequests();
        InstitutionRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Solicitação não encontrada."));
        if (request.getStatus() == Status.EXPIRED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Esta solicitação expirou após 1 semana e permanece apenas nos logs.");
        }
        if (request.getStatus() != Status.PENDING) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Esta solicitação já foi analisada.");
        }

        String action = decision.decision().trim().toUpperCase(Locale.ROOT);
        if ("APPROVE".equals(action)) {
            createInstitution(request, decision.institutionName());
            request.setStatus(Status.APPROVED);
        } else if ("REJECT".equals(action)) {
            request.setStatus(Status.REJECTED);
        } else {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "A decisão deve ser APPROVE ou REJECT.");
        }
        request.setProcessedAt(LocalDateTime.now());
        requestRepository.save(request);
        notificationRepository.findByReferenceTypeAndReferenceId(
                REQUEST_REFERENCE, requestId)
            .forEach(notification -> {
                notification.setRead(true);
                AdminNotification savedNotification = notificationRepository.save(notification);
        notifyAdminNotification(savedNotification);
            });
        return toRequestResponse(request);
    }

    @Transactional
    public TeacherResponseDTO decideTeacherRequest(
            Long teacherId,
            TeacherRequestDecisionDTO decision,
            String adminEmail) {
        expireAndSyncRequests();
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Solicitação de professor não encontrada."));

        Long scopedInstitutionId = resolveScopedInstitutionId(adminEmail);
        if (scopedInstitutionId != null) {
            Long teacherInstId = teacher.getInstitution() != null ? teacher.getInstitution().getId() : null;
            if (!Objects.equals(scopedInstitutionId, teacherInstId)) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você só pode aprovar ou recusar professores da sua própria instituição.");
            }
        }

        if ("EXPIRED".equalsIgnoreCase(teacher.getApprovalStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Esta solicitação de cadastro de professor expirou após 1 semana e permanece apenas nos logs.");
        }

        String action = decision.decision().trim().toUpperCase(Locale.ROOT);
        if ("APPROVE".equals(action)) {
            teacher.setApproved(true);
            teacher.setApprovalStatus("APPROVED");
        } else if ("REJECT".equals(action)) {
            if (teacher.isSuperAdmin() || teacher.isPrimaryInstitutionAdmin()) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta conta possui status elevado e não pode ser recusada.");
            }
            teacher.setApproved(false);
            teacher.setApprovalStatus("REJECTED");
        } else {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "A decisão deve ser APPROVE ou REJECT.");
        }

        Teacher saved = teacherRepository.save(teacher);
        if (userAuthCache != null) {
            userAuthCache.evict(teacherId);
        }
        if (sseEmitterService != null) {
            sseEmitterService.sendToUser(teacherId, "PROFILE_UPDATED", java.util.Map.of(
                "approvalStatus", saved.getApprovalStatus() != null ? saved.getApprovalStatus() : "UNKNOWN"
            ));
        }
        notificationRepository.findByReferenceTypeAndReferenceId(TEACHER_REFERENCE, teacherId)
            .forEach(notification -> {
                notification.setRead(true);
                AdminNotification savedNotification = notificationRepository.save(notification);
        notifyAdminNotification(savedNotification);
            });
        return DtoMapper.toTeacherResponse(saved);
    }

    private void expireAndSyncRequests() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(EXPIRATION_DAYS);

        List<InstitutionRequest> pendingRequests = requestRepository.findByStatusOrderByCreatedAtDesc(Status.PENDING);
        for (InstitutionRequest req : pendingRequests) {
            if (req.getCreatedAt() != null && req.getCreatedAt().isBefore(cutoff)) {
                req.setStatus(Status.EXPIRED);
                req.setProcessedAt(LocalDateTime.now());
                requestRepository.save(req);
            }
        }

        List<Teacher> allTeachers = teacherRepository.findAll();
        for (Teacher teacher : allTeachers) {
            if (teacher.isSuperAdmin()) continue;
            String status = teacher.getApprovalStatus();
            if (status == null || status.isBlank()) {
                status = Boolean.TRUE.equals(teacher.getApproved()) ? "APPROVED" : "PENDING";
                teacher.setApprovalStatus(status);
                teacherRepository.save(teacher);
            }
            if ("PENDING".equalsIgnoreCase(status)) {
                if (teacher.getCreatedAt() == null) {
                    teacher.setCreatedAt(LocalDateTime.now());
                    teacherRepository.save(teacher);
                } else if (teacher.getCreatedAt().isBefore(cutoff)) {
                    teacher.setApprovalStatus("EXPIRED");
                    teacher.setApproved(false);
                    teacherRepository.save(teacher);
                }
            }

            if (!Boolean.TRUE.equals(teacher.getApproved())
                    && notificationRepository.findByReferenceTypeAndReferenceId(TEACHER_REFERENCE, teacher.getId()).isEmpty()) {
                AdminNotification notification = new AdminNotification();
                notification.setType(TEACHER_REFERENCE);
                notification.setTitle("Solicitação de cadastro de professor");
                notification.setMessage(
                    teacher.getName() + " (" + teacher.getEmail() + ") solicitou cadastro como professor na instituição "
                    + (teacher.getInstitution() != null ? teacher.getInstitution().getName() : "informada") + "."
                );
                notification.setReferenceType(TEACHER_REFERENCE);
                notification.setReferenceId(teacher.getId());
                notification.setInstitutionId(teacher.getInstitution() != null ? teacher.getInstitution().getId() : null);
                AdminNotification savedNotification = notificationRepository.save(notification);
        notifyAdminNotification(savedNotification);
            }
        }
    }

    private Long resolveScopedInstitutionId(String adminEmail) {
        if (adminEmail == null || adminEmail.isBlank()) return null;
        User user = userRepository.findByEmailIgnoreCase(adminEmail.trim()).orElse(null);
        if (user == null || user.isSuperAdmin()) {
            return null;
        }
        Teacher teacher = teacherRepository.findById(user.getId()).orElse(null);
        if (teacher != null && teacher.getInstitution() != null) {
            return teacher.getInstitution().getId();
        }
        return null;
    }

    private void createInstitution(InstitutionRequest request, String chosenName) {
        String name = chosenName == null || chosenName.isBlank()
            ? request.getRequestedName()
            : chosenName.trim();
        Institution institution = Institution.builder()
            .name(name)
            .code("INST-REQ-" + request.getId())
            .email("instituicao-" + request.getId() + "@duolinfo.local")
            .city(request.getCity())
            .state(request.getState())
            .country("Brasil")
            .description(request.getMessage())
            .active(true)
            .build();
        institutionService.create(institution);
    }

    private AdminNotificationResponseDTO toNotificationResponse(
            AdminNotification notification) {
        String requestStatus = null;
        LocalDateTime baseTime = notification.getCreatedAt();

        if (REQUEST_REFERENCE.equalsIgnoreCase(notification.getReferenceType())
                && notification.getReferenceId() != null) {
            InstitutionRequest req = requestRepository.findById(notification.getReferenceId()).orElse(null);
            if (req != null) {
                requestStatus = req.getStatus().name();
                if (req.getCreatedAt() != null) {
                    baseTime = req.getCreatedAt();
                }
            }
        } else if (TEACHER_REFERENCE.equalsIgnoreCase(notification.getReferenceType())
                && notification.getReferenceId() != null) {
            Teacher teacher = teacherRepository.findById(notification.getReferenceId()).orElse(null);
            if (teacher != null) {
                requestStatus = teacher.getApprovalStatus() != null
                    ? teacher.getApprovalStatus()
                    : (Boolean.TRUE.equals(teacher.getApproved()) ? "APPROVED" : "PENDING");
                if (teacher.getCreatedAt() != null) {
                    baseTime = teacher.getCreatedAt();
                }
            }
        }

        LocalDateTime expiresAt = baseTime != null ? baseTime.plusDays(EXPIRATION_DAYS) : null;
        return new AdminNotificationResponseDTO(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.isRead(),
            notification.getReferenceType(),
            notification.getReferenceId(),
            notification.getCreatedAt(),
            notification.getInstitutionId(),
            requestStatus,
            expiresAt);
    }

    private InstitutionRequestResponseDTO toRequestResponse(InstitutionRequest request) {
        return new InstitutionRequestResponseDTO(
            request.getId(),
            request.getRequestedName(),
            request.getRequesterName(),
            request.getRequesterEmail(),
            request.getCity(),
            request.getState(),
            request.getMessage(),
            request.getStatus().name(),
            request.getCreatedAt());
    }

    private void notifyAdminNotification(AdminNotification notification) {
        if (notification == null || sseEmitterService == null) return;
        try {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("notificationId", notification.getId());
            data.put("type", notification.getType());
            data.put("title", notification.getTitle());
            data.put("message", notification.getMessage());
            data.put("institutionId", notification.getInstitutionId());
            sseEmitterService.sendToRole("ADMIN", "NOTIFICATION_ADDED", data);
        } catch (Exception ignored) {
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

