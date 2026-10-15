package com.duolinfo.ia.proj.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.duolinfo.ia.proj.dto.request.ExerciseFeedbackRequestDTO;
import com.duolinfo.ia.proj.dto.request.ExerciseSubmissionRequestDTO;
import com.duolinfo.ia.proj.dto.request.LearningExerciseRequestDTO;
import com.duolinfo.ia.proj.dto.response.ExerciseSubmissionResponseDTO;
import com.duolinfo.ia.proj.dto.response.LearningExerciseResponseDTO;
import com.duolinfo.ia.proj.dto.response.LearningRankingEntryResponseDTO;
import com.duolinfo.ia.proj.service.LearningExerciseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
@Tag(name = "Learning Exercises", description = "Atividades, respostas e feedback")
public class LearningExerciseController {

    private final LearningExerciseService exerciseService;

    @GetMapping
    @Operation(summary = "Lista atividades para o perfil autenticado")
    public List<LearningExerciseResponseDTO> findMine(
            Principal principal,
            Authentication authentication) {
        if (hasRole(authentication, "TEACHER")) {
            return exerciseService.findForTeacher(principal.getName());
        }
        return exerciseService.findForStudent(principal.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma atividade sem expor o gabarito ao estudante")
    public LearningExerciseResponseDTO findById(
            @PathVariable Long id,
            Principal principal) {
        return exerciseService.findById(id, principal.getName());
    }

    @PostMapping
    @Operation(summary = "Cria atividade para uma turma do professor")
    public LearningExerciseResponseDTO create(
            @Valid @RequestBody LearningExerciseRequestDTO request,
            Principal principal) {
        return exerciseService.create(request, principal.getName());
    }

    @PostMapping("/{id}/submissions")
    @Operation(summary = "Envia respostas e registra resultado e XP")
    public ExerciseSubmissionResponseDTO submit(
            @PathVariable Long id,
            @Valid @RequestBody ExerciseSubmissionRequestDTO request,
            Principal principal) {
        return exerciseService.submit(id, request.answers(), principal.getName());
    }

    @GetMapping("/submissions")
    @Operation(summary = "Lista histórico do estudante ou respostas das turmas do professor")
    public List<ExerciseSubmissionResponseDTO> findSubmissions(
            Principal principal) {
        return exerciseService.findSubmissions(principal.getName());
    }

    @GetMapping("/rankings/individual")
    @Operation(summary = "Ranking individual baseado em XP das entregas persistidas")
    public List<LearningRankingEntryResponseDTO> individualRanking(
            @RequestParam(defaultValue = "week") String period) {
        return exerciseService.findRanking(period, false);
    }

    @GetMapping("/rankings/groups")
    @Operation(summary = "Ranking de turmas baseado em XP das entregas persistidas")
    public List<LearningRankingEntryResponseDTO> groupRanking(
            @RequestParam(defaultValue = "week") String period) {
        return exerciseService.findRanking(period, true);
    }

    @PatchMapping("/submissions/{id}/feedback")
    @Operation(summary = "Registra feedback do professor para uma entrega")
    public ExerciseSubmissionResponseDTO addFeedback(
            @PathVariable Long id,
            @Valid @RequestBody ExerciseFeedbackRequestDTO request,
            Principal principal) {
        return exerciseService.addFeedback(id, request.feedback(), principal.getName());
    }

    private static boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
