package com.duolinfo.ia.proj.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.dto.ai.ExplainMistakeRequestDTO;
import com.duolinfo.ia.proj.dto.ai.ExplainMistakeResponseDTO;
import com.duolinfo.ia.proj.service.AiExplanationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AI Assistance", description = "Recursos de inteligência artificial e explicação pedagógica")
public class AiExplanationController {

    private final AiExplanationService aiExplanationService;

    @PostMapping("/explain-mistake")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Gera explicação pedagógica e dicas de memorização personalizadas para um erro cometido")
    public ExplainMistakeResponseDTO explainMistake(@Valid @RequestBody ExplainMistakeRequestDTO request) {
        return aiExplanationService.explainMistake(request);
    }
}
