package com.duolinfo.ia.proj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.duolinfo.ia.proj.config.UserAuthCache;
import com.duolinfo.ia.proj.controller.RealtimeController;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.service.AccessControlService;
import com.duolinfo.ia.proj.service.SseEmitterService;

class RealtimeAndCacheTests {

    @Test
    void userAuthCache_storesAndEvictsCorrectly() {
        UserAuthCache cache = new UserAuthCache();

        User user = new User();
        user.setId(101L);
        user.setEmail("user101@example.com");

        List<SimpleGrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ROLE_STUDENT"));

        cache.put(user, auths);

        UserAuthCache.CachedAuth cachedById = cache.getById(101L);
        assertNotNull(cachedById);
        assertEquals("user101@example.com", cachedById.user().getEmail());
        assertEquals(1, cachedById.authorities().size());

        UserAuthCache.CachedAuth cachedByEmail = cache.getByEmail("user101@example.com");
        assertNotNull(cachedByEmail);
        assertEquals(101L, cachedByEmail.user().getId());

        // Evict by user ID
        cache.evict(101L);
        assertNull(cache.getById(101L));
        assertNull(cache.getByEmail("user101@example.com"));

        // Put and evict by email
        cache.put(user, auths);
        assertNotNull(cache.getById(101L));
        cache.evictByEmail("USER101@example.com"); // Case insensitive check
        assertNull(cache.getById(101L));

        // Evict all
        cache.put(user, auths);
        cache.evictAll();
        assertNull(cache.getById(101L));
    }

    @Test
    void sseEmitterService_createsEmitterAndSendsEventsSafely() {
        SseEmitterService service = new SseEmitterService();

        SseEmitter emitter = service.createEmitter(200L, Set.of("STUDENT"));
        assertNotNull(emitter);
        assertEquals(1, service.getConnectedUsersCount());

        // Test sending to user
        service.sendToUser(200L, "EXERCISE_CREATED", Map.of("exerciseId", 1L));

        // Test sending to role
        service.sendToRole("STUDENT", "RANKING_UPDATED", Map.of("classId", 5L));

        // Test sending to other roles (should safely ignore without errors)
        service.sendToRole("ADMIN", "NOTIFICATION_ADDED", Map.of("id", 99L));

        // Test heartbeat ping
        service.sendHeartbeat();

        // Broadcast
        service.broadcast("SYSTEM_ALERT", Map.of("msg", "hello"));
    }

    @Test
    void realtimeController_returnsEmitterForAuthenticatedUser() {
        SseEmitterService service = new SseEmitterService();
        AccessControlService accessControlService = mock(AccessControlService.class);

        User user = new User();
        user.setId(300L);
        user.setEmail("teacher@example.com");

        when(accessControlService.getCurrentUserOrThrow()).thenReturn(user);

        RealtimeController controller = new RealtimeController(service, accessControlService);

        Authentication auth = new UsernamePasswordAuthenticationToken(
            user.getEmail(),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );

        SseEmitter emitter = controller.streamEvents(auth);
        assertNotNull(emitter);
        assertEquals(1, service.getConnectedUsersCount());
    }
}
