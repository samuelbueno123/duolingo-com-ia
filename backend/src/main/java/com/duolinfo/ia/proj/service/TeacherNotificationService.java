package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.response.StudentNotificationResponseDTO;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.TeacherNotification;
import com.duolinfo.ia.proj.repository.TeacherNotificationRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherNotificationService {

    private final TeacherNotificationRepository notificationRepository;
    private final TeacherRepository teacherRepository;
    private final SseEmitterService sseEmitterService;

    @Transactional
    public StudentNotificationResponseDTO notifyTeacher(
            Long teacherId,
            String title,
            String message,
            String type,
            String actionUrl) {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado."));

        TeacherNotification notif = TeacherNotification.builder()
            .teacher(teacher)
            .title(title)
            .message(message)
            .type(type != null ? type : "GENERAL")
            .read(false)
            .actionUrl(actionUrl)
            .createdAt(LocalDateTime.now())
            .build();

        notif = notificationRepository.save(notif);

        if (sseEmitterService != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("teacherId", teacherId);
            payload.put("notificationId", notif.getId());
            payload.put("title", title);
            payload.put("message", message);
            payload.put("type", notif.getType());
            if (actionUrl != null) {
                payload.put("actionUrl", actionUrl);
            }
            sseEmitterService.sendToUser(teacherId, "NOTIFICATION_ADDED", payload);
        }

        return mapToDTO(notif);
    }

    @Transactional(readOnly = true)
    public List<StudentNotificationResponseDTO> getNotifications(Long teacherId) {
        return notificationRepository.findByTeacher_IdOrderByCreatedAtDesc(teacherId)
            .stream()
            .map(this::mapToDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long teacherId) {
        return notificationRepository.countByTeacher_IdAndReadFalse(teacherId);
    }

    @Transactional
    public StudentNotificationResponseDTO markAsRead(Long teacherId, Long notificationId) {
        TeacherNotification notif = notificationRepository.findByIdAndTeacher_Id(notificationId, teacherId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada."));

        notif.setRead(true);
        notif = notificationRepository.save(notif);
        if (sseEmitterService != null) {
            sseEmitterService.sendToUser(teacherId, "NOTIFICATION_UPDATED", Map.of(
                "teacherId", teacherId,
                "notificationId", notificationId
            ));
        }
        return mapToDTO(notif);
    }

    @Transactional
    public void markAllAsRead(Long teacherId) {
        List<TeacherNotification> list = notificationRepository.findByTeacher_IdOrderByCreatedAtDesc(teacherId);
        for (TeacherNotification n : list) {
            if (!n.isRead()) {
                n.setRead(true);
            }
        }
        notificationRepository.saveAll(list);
        if (sseEmitterService != null) {
            sseEmitterService.sendToUser(teacherId, "NOTIFICATION_UPDATED", Map.of(
                "teacherId", teacherId,
                "allRead", true
            ));
        }
    }

    private StudentNotificationResponseDTO mapToDTO(TeacherNotification n) {
        return new StudentNotificationResponseDTO(
            n.getId(),
            n.getTitle(),
            n.getMessage(),
            n.getType(),
            n.isRead(),
            n.getActionUrl(),
            n.getCreatedAt()
        );
    }
}
