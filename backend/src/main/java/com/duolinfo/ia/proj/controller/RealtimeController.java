package com.duolinfo.ia.proj.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.service.AccessControlService;
import com.duolinfo.ia.proj.service.SseEmitterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/realtime")
@Tag(name = "Realtime", description = "Canal de sincronização e eventos em tempo real via Server-Sent Events (SSE)")
public class RealtimeController {

    private final SseEmitterService sseEmitterService;
    private final AccessControlService accessControlService;

    public RealtimeController(
            SseEmitterService sseEmitterService,
            AccessControlService accessControlService) {
        this.sseEmitterService = sseEmitterService;
        this.accessControlService = accessControlService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Abre conexão contínua de eventos (SSE)",
        description = "Permite que a interface receba atualizações push instantâneas quando dados forem alterados no banco de dados."
    )
    public SseEmitter streamEvents(Authentication authentication) {
        User currentUser = accessControlService.getCurrentUserOrThrow();

        Set<String> roles = authentication.getAuthorities().stream()
            .map(auth -> auth.getAuthority().replaceFirst("^ROLE_", ""))
            .collect(Collectors.toSet());

        return sseEmitterService.createEmitter(currentUser.getId(), roles);
    }
}
