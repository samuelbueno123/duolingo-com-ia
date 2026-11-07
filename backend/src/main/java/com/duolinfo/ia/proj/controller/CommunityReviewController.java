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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.community.CommunityReportRequestDTO;
import com.duolinfo.ia.proj.dto.community.CommunityReviewRequestDTO;
import com.duolinfo.ia.proj.dto.community.CommunityReviewResponseDTO;
import com.duolinfo.ia.proj.dto.community.CommunitySubmissionRequestDTO;
import com.duolinfo.ia.proj.dto.community.CommunitySubmissionResponseDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.service.CommunityReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Community Reviews", description = "Correção colaborativa de exercícios por pares com controle de fluência")
public class CommunityReviewController {

    private final CommunityReviewService communityService;
    private final StudentRepository studentRepository;

    private Student resolveStudent(Authentication auth) {
        return studentRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de estudante não encontrado."));
    }

    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envia uma resposta de exercício para avaliação de outros membros da comunidade")
    public CommunitySubmissionResponseDTO createSubmission(
            @Valid @RequestBody CommunitySubmissionRequestDTO dto,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return communityService.createSubmission(student.getId(), dto);
    }

    @GetMapping("/feed")
    @Operation(summary = "Lista exercícios de outros estudantes aguardando revisão colaborativa")
    public List<CommunitySubmissionResponseDTO> getFeed(
            @RequestParam(required = false) String language,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return communityService.getCommunityFeed(student.getId(), language);
    }

    @GetMapping("/my-submissions")
    @Operation(summary = "Lista os exercícios submetidos pelo estudante logado com feedbacks recebidos")
    public List<CommunitySubmissionResponseDTO> getMySubmissions(Authentication authentication) {
        Student student = resolveStudent(authentication);
        return communityService.getMySubmissions(student.getId());
    }

    @GetMapping("/submissions/{id}")
    @Operation(summary = "Recupera detalhes de um exercício e suas avaliações comunitárias")
    public CommunitySubmissionResponseDTO getSubmissionDetails(@PathVariable Long id) {
        return communityService.getSubmissionDetails(id);
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envia uma revisão com correções, dicas de expressões nativas e pontuação")
    public CommunityReviewResponseDTO addReview(
            @Valid @RequestBody CommunityReviewRequestDTO dto,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return communityService.addReview(student.getId(), dto);
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Denuncia uma avaliação inadequada ou ofensiva para moderação")
    public void reportReview(
            @Valid @RequestBody CommunityReportRequestDTO dto,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        communityService.reportReview(student.getId(), dto);
    }
}
