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

import com.duolinfo.ia.proj.dto.ai.AiConversationMessageRequestDTO;
import com.duolinfo.ia.proj.dto.ai.AiConversationSessionResponseDTO;
import com.duolinfo.ia.proj.dto.ai.AiConversationStartDTO;
import com.duolinfo.ia.proj.dto.ai.AiMistakeSummaryDTO;
import com.duolinfo.ia.proj.dto.ai.AiScenarioDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.service.AiConversationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/conversations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AI Conversation", description = "Prática de conversação contextualizada e interativa com Inteligência Artificial")
public class AiConversationController {

    private final AiConversationService aiConversationService;
    private final StudentRepository studentRepository;

    private Student resolveStudent(Authentication auth) {
        return studentRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de estudante não encontrado para este usuário."));
    }

    @GetMapping("/scenarios")
    @Operation(summary = "Lista os cenários de simulação disponíveis (aeroporto, restaurante, etc.)")
    public List<AiScenarioDTO> getScenarios(@RequestParam(required = false) String cefrLevel) {
        return aiConversationService.getScenarios(cefrLevel);
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inicia uma nova sessão de conversação com o assistente IA em um cenário escolhido")
    public AiConversationSessionResponseDTO startSession(
            @Valid @RequestBody AiConversationStartDTO request,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return aiConversationService.startSession(student.getId(), request);
    }

    @PostMapping("/{sessionId}/messages")
    @Operation(summary = "Envia uma fala do estudante e recebe a resposta da IA com correções gramaticais e sugestões nativas")
    public AiConversationSessionResponseDTO sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody AiConversationMessageRequestDTO request,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return aiConversationService.sendMessage(student.getId(), sessionId, request);
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Recupera o histórico completo de uma sessão de conversação")
    public AiConversationSessionResponseDTO getSession(
            @PathVariable Long sessionId,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return aiConversationService.getSessionDetails(student.getId(), sessionId);
    }

    @GetMapping("/my-sessions")
    @Operation(summary = "Lista as sessões de conversação do estudante autenticado")
    public List<AiConversationSessionResponseDTO> getMySessions(
            @RequestParam(required = false) String targetLanguage,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return aiConversationService.getMySessions(student.getId(), targetLanguage);
    }

    @GetMapping("/my-mistakes")
    @Operation(summary = "Recupera o histórico de correções gramaticais e sugestões pedagógicas recebidas nas conversas")
    public List<AiMistakeSummaryDTO> getMyMistakes(Authentication authentication) {
        Student student = resolveStudent(authentication);
        return aiConversationService.getMyMistakes(student.getId());
    }

    @PostMapping("/{sessionId}/end")
    @Operation(summary = "Encerra a sessão de conversação ativa")
    public AiConversationSessionResponseDTO endSession(
            @PathVariable Long sessionId,
            Authentication authentication) {
        Student student = resolveStudent(authentication);
        return aiConversationService.endSession(student.getId(), sessionId);
    }
}
