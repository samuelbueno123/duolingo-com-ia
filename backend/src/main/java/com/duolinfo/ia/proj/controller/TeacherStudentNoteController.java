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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.TeacherStudentNoteRequestDTO;
import com.duolinfo.ia.proj.dto.response.TeacherStudentNoteResponseDTO;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.service.TeacherStudentNoteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teachers/me/notes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher Private Notes", description = "Anotações pedagógicas privadas do professor sobre alunos e turmas")
public class TeacherStudentNoteController {

    private final TeacherStudentNoteService noteService;
    private final TeacherRepository teacherRepository;

    private Teacher resolveTeacher(Principal principal) {
        return teacherRepository.findByEmailIgnoreCase(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado."));
    }

    @GetMapping
    @Operation(summary = "Lista as anotações privadas do professor sobre um estudante ou turma")
    public List<TeacherStudentNoteResponseDTO> getNotes(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId,
            Principal principal) {
        Teacher teacher = resolveTeacher(principal);
        return noteService.getNotes(teacher.getId(), studentId, classId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma anotação pedagógica confidencial sobre o estudante")
    public TeacherStudentNoteResponseDTO saveNote(
            @Valid @RequestBody TeacherStudentNoteRequestDTO request,
            Principal principal) {
        Teacher teacher = resolveTeacher(principal);
        return noteService.saveNote(teacher.getId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma anotação pedagógica")
    public void deleteNote(@PathVariable Long id, Principal principal) {
        Teacher teacher = resolveTeacher(principal);
        noteService.deleteNote(teacher.getId(), id);
    }
}
