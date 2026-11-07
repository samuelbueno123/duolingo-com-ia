package com.duolinfo.ia.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.dto.srs.SpacedReviewAnswerDTO;
import com.duolinfo.ia.proj.dto.srs.SpacedReviewItemRequestDTO;
import com.duolinfo.ia.proj.dto.srs.SpacedReviewItemResponseDTO;
import com.duolinfo.ia.proj.dto.srs.SpacedReviewSessionSummaryDTO;
import com.duolinfo.ia.proj.service.SpacedRepetitionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/srs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Spaced Repetition", description = "Sistema de repetição espaçada (SM-2) para revisão de vocabulário")
public class SpacedRepetitionController {

    private final SpacedRepetitionService srsService;

    @GetMapping("/session")
    @Operation(summary = "Retorna a sessão de flashcards vencidos para revisão hoje")
    public List<SpacedReviewItemResponseDTO> getSession(
            @RequestParam(required = false) String language,
            Authentication authentication) {
        return srsService.getDueSession(authentication.getName(), language);
    }

    @PostMapping("/review/{itemId}")
    @Operation(summary = "Registra a resposta da revisão de um card com base no algoritmo SM-2")
    public SpacedReviewItemResponseDTO reviewCard(
            @PathVariable Long itemId,
            @Valid @RequestBody SpacedReviewAnswerDTO answer,
            Authentication authentication) {
        return srsService.reviewItem(authentication.getName(), itemId, answer);
    }

    @GetMapping("/items")
    @Operation(summary = "Lista todos os cards de vocabulário do estudante")
    public List<SpacedReviewItemResponseDTO> listItems(
            @RequestParam(required = false) String language,
            Authentication authentication) {
        return srsService.listItems(authentication.getName(), language);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma nova palavra ou expressão para revisão espaçada")
    public SpacedReviewItemResponseDTO createItem(
            @Valid @RequestBody SpacedReviewItemRequestDTO request,
            Authentication authentication) {
        return srsService.createItem(authentication.getName(), request);
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um card de vocabulário")
    public void deleteItem(
            @PathVariable Long itemId,
            Authentication authentication) {
        srsService.deleteItem(authentication.getName(), itemId);
    }

    @GetMapping("/stats")
    @Operation(summary = "Retorna estatísticas gerais de retenção e progresso da repetição espaçada")
    public SpacedReviewSessionSummaryDTO getStats(Authentication authentication) {
        return srsService.getStats(authentication.getName());
    }
}
