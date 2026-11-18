package com.duolinfo.ia.proj.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.response.StudentNotificationResponseDTO;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.service.TeacherNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teachers/me/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher Notifications", description = "Notificações do professor para correções pendentes e avisos acadêmicos")
public class TeacherNotificationController {

    private final TeacherNotificationService notificationService;
    private final TeacherRepository teacherRepository;

    private Teacher resolveTeacher(Principal principal) {
        return teacherRepository.findByEmailIgnoreCase(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado."));
    }

    @GetMapping
    @Operation(summary = "Lista todas as notificações recebidas pelo professor")
    public List<StudentNotificationResponseDTO> getNotifications(Principal principal) {
        Teacher teacher = resolveTeacher(principal);
        return notificationService.getNotifications(teacher.getId());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Retorna a contagem de notificações não lidas do professor")
    public Map<String, Long> getUnreadCount(Principal principal) {
        Teacher teacher = resolveTeacher(principal);
        long count = notificationService.getUnreadCount(teacher.getId());
        return Map.of("unreadCount", count);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marca uma notificação do professor como lida")
    public StudentNotificationResponseDTO markAsRead(@PathVariable Long id, Principal principal) {
        Teacher teacher = resolveTeacher(principal);
        return notificationService.markAsRead(teacher.getId(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Marca todas as notificações do professor como lidas")
    public Map<String, String> markAllAsRead(Principal principal) {
        Teacher teacher = resolveTeacher(principal);
        notificationService.markAllAsRead(teacher.getId());
        return Map.of("message", "Todas as notificações foram marcadas como lidas.");
    }
}
