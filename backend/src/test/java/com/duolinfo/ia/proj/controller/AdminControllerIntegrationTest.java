package com.duolinfo.ia.proj.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.duolinfo.ia.proj.dto.response.AdminNotificationResponseDTO;
import com.duolinfo.ia.proj.dto.response.AdminTableMetadataResponseDTO;
import com.duolinfo.ia.proj.dto.response.AdminUserResponseDTO;
import com.duolinfo.ia.proj.service.AchievementService;
import com.duolinfo.ia.proj.service.AdminService;
import com.duolinfo.ia.proj.service.InstitutionRequestService;

class AdminControllerIntegrationTest {

    private MockMvc mockMvc;
    private AdminService adminService;
    private InstitutionRequestService institutionRequestService;
    private AchievementService achievementService;

    @BeforeEach
    void setUp() {
        adminService = mock(AdminService.class);
        institutionRequestService = mock(InstitutionRequestService.class);
        achievementService = mock(AchievementService.class);

        AdminController controller = new AdminController(
            adminService,
            institutionRequestService,
            achievementService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void notifications_returnsNotificationList() throws Exception {
        AdminNotificationResponseDTO notif = new AdminNotificationResponseDTO(
            1L,
            "TEACHER_REQUEST",
            "Solicitação de Professor",
            "Prof. Silva solicitou cadastro",
            false,
            "TEACHER_REQUEST",
            10L,
            LocalDateTime.now(),
            1L,
            "PENDING",
            LocalDateTime.now().plusDays(7)
        );

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "admin@duolingo.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        when(institutionRequestService.notifications("admin@duolingo.com")).thenReturn(List.of(notif));

        mockMvc.perform(get("/api/admin/notifications").principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].title", is("Solicitação de Professor")))
            .andExpect(jsonPath("$[0].requestStatus", is("PENDING")));
    }

    @Test
    void listTables_returnsMetadataList() throws Exception {
        AdminTableMetadataResponseDTO meta = new AdminTableMetadataResponseDTO(
            "users",
            "Usuários",
            150L,
            List.of("id"),
            List.of(),
            false
        );

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "admin@duolingo.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        when(adminService.listTables()).thenReturn(List.of(meta));

        mockMvc.perform(get("/api/admin/tables").principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tableName", is("users")))
            .andExpect(jsonPath("$[0].rowCount", is(150)));

        verify(adminService).requireSuperAdmin("admin@duolingo.com");
    }

    @Test
    void listUsers_returnsAdminUserResponseList() throws Exception {
        AdminUserResponseDTO user = new AdminUserResponseDTO(
            1L,
            "Administrador Principal",
            "admin@duolingo.com",
            true,
            "ADMIN",
            "ROLE_ADMIN",
            "g-admin",
            null,
            "MASCULINO",
            true,
            true,
            true,
            1L,
            "CEFET Nova Iguaçu"
        );

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "admin@duolingo.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        when(adminService.listUsers("admin@duolingo.com")).thenReturn(List.of(user));

        mockMvc.perform(get("/api/admin/users").principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name", is("Administrador Principal")))
            .andExpect(jsonPath("$[0].superAdmin", is(true)));
    }

    @Test
    void markNotificationRead_delegatesToService() throws Exception {
        AdminNotificationResponseDTO notif = new AdminNotificationResponseDTO(
            5L,
            "INFO",
            "Atualização",
            "Sistema atualizado",
            true,
            null,
            null,
            LocalDateTime.now(),
            null,
            null,
            null
        );

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "admin@duolingo.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        when(institutionRequestService.markRead(5L)).thenReturn(notif);

        mockMvc.perform(patch("/api/admin/notifications/5/read").principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.read", is(true)));
    }
}
