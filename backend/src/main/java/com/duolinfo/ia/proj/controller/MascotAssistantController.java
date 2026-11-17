package com.duolinfo.ia.proj.controller;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.dto.request.MascotChatRequestDTO;
import com.duolinfo.ia.proj.dto.response.MascotChatResponseDTO;
import com.duolinfo.ia.proj.service.MascotAiAssistantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/mascot")
@RequiredArgsConstructor
@Tag(name = "Mascot Assistant", description = "Chat contextual do mascote Bode Branco restrito à plataforma")
public class MascotAssistantController {

    private final MascotAiAssistantService mascotAiAssistantService;

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Responde dúvidas sobre uso e navegação na plataforma Duolinfo")
    public MascotChatResponseDTO chat(
            @Valid @RequestBody MascotChatRequestDTO request,
            Principal principal) {
        String email = principal != null ? principal.getName() : "anon";
        return mascotAiAssistantService.chat(request, email);
    }
}
