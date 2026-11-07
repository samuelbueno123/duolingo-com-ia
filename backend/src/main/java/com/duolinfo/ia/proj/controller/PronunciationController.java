package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.pronunciation.PronunciationChallengeDTO;
import com.duolinfo.ia.proj.dto.pronunciation.PronunciationEvaluationRequestDTO;
import com.duolinfo.ia.proj.dto.pronunciation.PronunciationEvaluationResponseDTO;
import com.duolinfo.ia.proj.dto.pronunciation.PronunciationStatsDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.service.PronunciationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pronunciation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pronunciation", description = "Avaliação de pronúncia fonética e prática oral interativa")
public class PronunciationController {

    private final PronunciationService pronunciationService;
    private final StudentRepository studentRepository;

    private Student resolveStudent(Authentication auth) {
        return studentRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de estudante não encontrado."));
    }

    @GetMapping("/challenges")
    @Operation(summary = "Lista desafios de pronúncia por idioma e nível CEFR")
    public List<PronunciationChallengeDTO> getChallenges(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String cefrLevel) {
        return pronunciationService.getChallenges(language, cefrLevel);
    }

    @GetMapping("/challenges/{id}")
    @Operation(summary = "Obtém detalhes de um desafio de pronúncia")
    public PronunciationChallengeDTO getChallenge(@PathVariable Long id) {
        return pronunciationService.getChallengeById(id);
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Avalia a gravação/transcrição da fala do estudante contra a frase esperada")
    public PronunciationEvaluationResponseDTO evaluate(
            @Valid @RequestBody PronunciationEvaluationRequestDTO request,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return pronunciationService.evaluateAttempt(student.getId(), request);
    }

    @GetMapping("/history")
    @Operation(summary = "Retorna histórico de tentativas do estudante")
    public List<PronunciationEvaluationResponseDTO> getHistory(
            @RequestParam(required = false) Long challengeId,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return pronunciationService.getStudentHistory(student.getId(), challengeId);
    }

    @GetMapping("/stats")
    @Operation(summary = "Retorna estatísticas consolidadas de pronúncia do estudante")
    public PronunciationStatsDTO getStats(Authentication authentication) {
        Student student = resolveStudent(authentication);
        return pronunciationService.getStudentStats(student.getId());
    }
}
