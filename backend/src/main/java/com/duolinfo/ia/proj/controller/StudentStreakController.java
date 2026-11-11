package com.duolinfo.ia.proj.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.DailyGoalRequestDTO;
import com.duolinfo.ia.proj.dto.response.StreakResponseDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.service.StudentStreakService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students/me/streak")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Student Streak & Goals", description = "Sequência de estudos diários (ofensiva), bloqueios de streak e metas diárias")
public class StudentStreakController {

    private final StudentStreakService streakService;
    private final StudentRepository studentRepository;

    private Student resolveStudent(Principal principal) {
        return studentRepository.findByEmailIgnoreCase(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado."));
    }

    @GetMapping
    @Operation(summary = "Obtém dados da ofensiva (streak), bloqueios e meta diária do estudante")
    public StreakResponseDTO getStreak(Principal principal) {
        Student student = resolveStudent(principal);
        return streakService.getStreak(student.getId());
    }

    @PostMapping("/goal")
    @Operation(summary = "Atualiza a meta diária de XP de estudo")
    public StreakResponseDTO setDailyGoal(
            @Valid @RequestBody DailyGoalRequestDTO request,
            Principal principal) {
        Student student = resolveStudent(principal);
        return streakService.setDailyGoal(student.getId(), request.dailyXpGoal());
    }

    @PostMapping("/freeze/buy")
    @Operation(summary = "Adquire um bloqueio de ofensiva (streak freeze) para proteger o progresso")
    public StreakResponseDTO buyStreakFreeze(Principal principal) {
        Student student = resolveStudent(principal);
        return streakService.buyStreakFreeze(student.getId());
    }
}
