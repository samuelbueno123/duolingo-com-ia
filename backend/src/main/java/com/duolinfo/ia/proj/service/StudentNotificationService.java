package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.response.StudentNotificationResponseDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentNotification;
import com.duolinfo.ia.proj.repository.StudentNotificationRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentNotificationService {

    private final StudentNotificationRepository notificationRepository;
    private final StudentRepository studentRepository;
    private final SseEmitterService sseEmitterService;

    @Transactional
    public StudentNotificationResponseDTO notifyStudent(
            Long studentId,
            String title,
            String message,
            String type,
            String actionUrl) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado."));

        StudentNotification notif = StudentNotification.builder()
            .student(student)
            .title(title)
            .message(message)
            .type(type != null ? type : "GENERAL")
            .read(false)
            .actionUrl(actionUrl)
            .createdAt(LocalDateTime.now())
            .build();

        notif = notificationRepository.save(notif);

        // Push real-time event to connected frontend
        if (sseEmitterService != null) {
            sseEmitterService.sendToUser(studentId, "NOTIFICATION_ADDED", Map.of(
                "studentId", studentId,
                "title", title,
                "type", notif.getType()
            ));
        }

        return mapToDTO(notif);
    }

    @Transactional(readOnly = true)
    public List<StudentNotificationResponseDTO> getNotifications(Long studentId) {
        return notificationRepository.findByStudent_IdOrderByCreatedAtDesc(studentId)
            .stream()
            .map(this::mapToDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long studentId) {
        return notificationRepository.countByStudent_IdAndReadFalse(studentId);
    }

    @Transactional
    public StudentNotificationResponseDTO markAsRead(Long studentId, Long notificationId) {
        StudentNotification notif = notificationRepository.findByIdAndStudent_Id(notificationId, studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada."));

        notif.setRead(true);
        notif = notificationRepository.save(notif);
        return mapToDTO(notif);
    }

    @Transactional
    public void markAllAsRead(Long studentId) {
        List<StudentNotification> list = notificationRepository.findByStudent_IdOrderByCreatedAtDesc(studentId);
        for (StudentNotification n : list) {
            if (!n.isRead()) {
                n.setRead(true);
            }
        }
        notificationRepository.saveAll(list);
    }

    private StudentNotificationResponseDTO mapToDTO(StudentNotification n) {
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
