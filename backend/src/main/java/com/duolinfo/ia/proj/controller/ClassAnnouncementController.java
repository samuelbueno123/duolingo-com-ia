package com.duolinfo.ia.proj.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.ClassAnnouncementRequestDTO;
import com.duolinfo.ia.proj.dto.response.ClassAnnouncementResponseDTO;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.service.ClassAnnouncementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Class Announcements", description = "Mural de avisos e comunicados da turma com notificações automáticas")
public class ClassAnnouncementController {

    private final ClassAnnouncementService announcementService;
    private final TeacherRepository teacherRepository;

    private Teacher resolveTeacher(Principal principal) {
        return teacherRepository.findByEmailIgnoreCase(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado."));
    }

    @GetMapping("/{classId}/announcements")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista todos os comunicados de uma turma")
    public List<ClassAnnouncementResponseDTO> getAnnouncements(@PathVariable Long classId) {
        return announcementService.getAnnouncementsByClass(classId);
    }

    @PostMapping("/{classId}/announcements")
    @PreAuthorize("hasRole('TEACHER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publica um novo comunicado para a turma")
    public ClassAnnouncementResponseDTO createAnnouncement(
            @PathVariable Long classId,
            @Valid @RequestBody ClassAnnouncementRequestDTO request,
            Principal principal) {
        Teacher teacher = resolveTeacher(principal);
        return announcementService.createAnnouncement(teacher.getId(), classId, request);
    }

    @DeleteMapping("/announcements/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um comunicado da turma")
    public void deleteAnnouncement(
            @PathVariable Long id,
            Principal principal) {
        Teacher teacher = resolveTeacher(principal);
        announcementService.deleteAnnouncement(teacher.getId(), id);
    }
}
