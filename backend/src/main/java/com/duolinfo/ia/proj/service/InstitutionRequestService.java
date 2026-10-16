package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.InstitutionRequestCreateDTO;
import com.duolinfo.ia.proj.dto.request.InstitutionRequestDecisionDTO;
import com.duolinfo.ia.proj.dto.response.AdminNotificationResponseDTO;
import com.duolinfo.ia.proj.dto.response.InstitutionRequestResponseDTO;
import com.duolinfo.ia.proj.entity.AdminNotification;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.InstitutionRequest;
import com.duolinfo.ia.proj.entity.InstitutionRequest.Status;
import com.duolinfo.ia.proj.repository.AdminNotificationRepository;
import com.duolinfo.ia.proj.repository.InstitutionRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstitutionRequestService {

    private static final String REQUEST_REFERENCE = "INSTITUTION_REQUEST";

    private final InstitutionRequestRepository requestRepository;
    private final AdminNotificationRepository notificationRepository;
    private final InstitutionService institutionService;

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
        notificationRepository.save(notification);

        return toRequestResponse(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<AdminNotificationResponseDTO> notifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toNotificationResponse)
            .toList();
    }

    @Transactional
    public AdminNotificationResponseDTO markRead(Long notificationId) {
        AdminNotification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Notificação não encontrada."));
        notification.setRead(true);
        return toNotificationResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public List<InstitutionRequestResponseDTO> requests(Status status) {
        List<InstitutionRequest> requests = status == null
            ? requestRepository.findAllByOrderByCreatedAtDesc()
            : requestRepository.findByStatusOrderByCreatedAtDesc(status);
        return requests.stream().map(this::toRequestResponse).toList();
    }

    @Transactional
    public InstitutionRequestResponseDTO decide(
            Long requestId,
            InstitutionRequestDecisionDTO decision) {
        InstitutionRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Solicitação não encontrada."));
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
                notificationRepository.save(notification);
            });
        return toRequestResponse(request);
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
        return new AdminNotificationResponseDTO(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.isRead(),
            notification.getReferenceType(),
            notification.getReferenceId(),
            notification.getCreatedAt());
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

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
