package com.duolinfo.ia.proj.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.MistakeReviewRequestDTO;
import com.duolinfo.ia.proj.dto.response.StudentMistakeResponseDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.service.StudentMistakeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students/me/mistakes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Student Mistakes (Caderno de Erros)", description = "Gerenciamento inteligente de erros e repetição focada até domínio")
public class StudentMistakeController {

    private final StudentMistakeService mistakeService;
    private final StudentRepository studentRepository;

    private Student resolveStudent(Principal principal) {
        return studentRepository.findByEmailIgnoreCase(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado."));
    }

    @GetMapping
    @Operation(summary = "Lista questões erradas pendentes de revisão e domínio")
    public List<StudentMistakeResponseDTO> getUnresolvedMistakes(Principal principal) {
        Student student = resolveStudent(principal);
        return mistakeService.getUnresolvedMistakes(student.getId());
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "Registra a tentativa de revisão de uma questão errada")
    public StudentMistakeResponseDTO reviewMistake(
            @PathVariable Long id,
            @Valid @RequestBody MistakeReviewRequestDTO request,
            Principal principal) {
        Student student = resolveStudent(principal);
        return mistakeService.reviewMistake(student.getId(), id, request.isCorrect());
    }

    @GetMapping("/stats")
    @Operation(summary = "Retorna métricas de erros, pendências e domínio do estudante")
    public Map<String, Object> getStats(Principal principal) {
        Student student = resolveStudent(principal);
        return mistakeService.getStats(student.getId());
    }
}
