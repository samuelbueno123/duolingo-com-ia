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
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.service.StudentNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students/me/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Student Notifications", description = "Central unificada de notificações pedagógicas, avisos de turmas e prazos")
public class StudentNotificationController {

    private final StudentNotificationService notificationService;
    private final StudentRepository studentRepository;

    private Student resolveStudent(Principal principal) {
        return studentRepository.findByEmailIgnoreCase(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado."));
    }

    @GetMapping
    @Operation(summary = "Lista todas as notificações recebidas pelo estudante")
    public List<StudentNotificationResponseDTO> getNotifications(Principal principal) {
        Student student = resolveStudent(principal);
        return notificationService.getNotifications(student.getId());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Retorna a contagem de notificações pendentes de leitura")
    public Map<String, Long> getUnreadCount(Principal principal) {
        Student student = resolveStudent(principal);
        long count = notificationService.getUnreadCount(student.getId());
        return Map.of("unreadCount", count);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marca uma notificação individual como lida")
    public StudentNotificationResponseDTO markAsRead(@PathVariable Long id, Principal principal) {
        Student student = resolveStudent(principal);
        return notificationService.markAsRead(student.getId(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Marca todas as notificações pendentes como lidas")
    public Map<String, String> markAllAsRead(Principal principal) {
        Student student = resolveStudent(principal);
        notificationService.markAllAsRead(student.getId());
        return Map.of("message", "Todas as notificações foram marcadas como lidas.");
    }
}
