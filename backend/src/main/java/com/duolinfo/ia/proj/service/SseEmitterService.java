package com.duolinfo.ia.proj.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Serviço de comunicação em tempo real via Server-Sent Events (SSE).
 * Gerencia canais de eventos unidirecionais do servidor para clientes conectados,
 * permitindo que a interface seja notificada instantaneamente quando dados mudarem no banco.
 */
@Service
public class SseEmitterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SseEmitterService.class);
    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L; // 30 minutos

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userRoles = new ConcurrentHashMap<>();

    /**
     * Cria e registra um novo emitter SSE para o usuário autenticado.
     */
    public SseEmitter createEmitter(Long userId, Set<String> roles) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        if (roles != null && !roles.isEmpty()) {
            userRoles.put(userId, roles);
        }

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            removeEmitter(userId, emitter);
        });
        emitter.onError(e -> removeEmitter(userId, emitter));

        // Envia confirmação inicial de conexão
        try {
            Map<String, Object> ack = new LinkedHashMap<>();
            ack.put("type", "CONNECTED");
            ack.put("userId", userId);
            ack.put("timestamp", System.currentTimeMillis());
            ack.put("message", "Conexão de sincronização em tempo real estabelecida com sucesso.");

            emitter.send(SseEmitter.event()
                .name("CONNECTED")
                .data(ack));
        } catch (IOException e) {
            removeEmitter(userId, emitter);
        }

        LOGGER.debug("SSE conectado para o usuário ID: {}", userId);
        return emitter;
    }

    /**
     * Envia um evento em tempo real para um usuário específico.
     */
    public void sendToUser(Long userId, String eventType, Object data) {
        if (userId == null) return;
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) return;

        Map<String, Object> payload = buildPayload(eventType, data);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventType).data(payload));
            } catch (Exception e) {
                removeEmitter(userId, emitter);
            }
        }
    }

    /**
     * Envia um evento para múltiplos usuários.
     */
    public void sendToUsers(Collection<Long> userIds, String eventType, Object data) {
        if (userIds == null || userIds.isEmpty()) return;
        userIds.forEach(userId -> sendToUser(userId, eventType, data));
    }

    /**
     * Envia um evento para todos os usuários que possuem determinado papel (ex: ADMIN, TEACHER).
     */
    public void sendToRole(String role, String eventType, Object data) {
        if (role == null || role.isBlank()) return;
        String cleanRole = role.replaceFirst("^ROLE_", "").toUpperCase();

        userRoles.forEach((userId, roles) -> {
            boolean hasRole = roles.stream()
                .anyMatch(r -> r.replaceFirst("^ROLE_", "").equalsIgnoreCase(cleanRole));
            if (hasRole) {
                sendToUser(userId, eventType, data);
            }
        });
    }

    /**
     * Envia um evento para todos os clientes conectados (broadcast geral).
     */
    public void broadcast(String eventType, Object data) {
        Map<String, Object> payload = buildPayload(eventType, data);

        userEmitters.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name(eventType).data(payload));
                } catch (Exception e) {
                    removeEmitter(userId, emitter);
                }
            }
        });
    }

    /**
     * Heartbeat periódico (a cada 25s) para manter os proxies e firewalls ativos
     * e descartar clientes inativos sem consumir recursos desnecessários.
     */
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        userEmitters.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    removeEmitter(userId, emitter);
                }
            }
        });
    }

    public int getConnectedUsersCount() {
        return userEmitters.size();
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
                userRoles.remove(userId);
            }
        }
    }

    private Map<String, Object> buildPayload(String eventType, Object data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", eventType);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("data", data != null ? data : Collections.emptyMap());
        return payload;
    }
}
