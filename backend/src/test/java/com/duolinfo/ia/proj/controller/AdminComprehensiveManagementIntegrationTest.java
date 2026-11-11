package com.duolinfo.ia.proj.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.entity.CatalogOption;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.repository.CatalogOptionRepository;
import com.duolinfo.ia.proj.repository.UserRepository;
import com.duolinfo.ia.proj.service.AuditLogService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminComprehensiveManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CatalogOptionRepository catalogOptionRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Test
    @WithMockUser(username = "admin@duolingo.com", roles = {"ADMIN"})
    @DisplayName("Admin: Consulta métricas globais no dashboard executivo (/api/admin/dashboard/overview)")
    void testGetDashboardOverview() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalUsers", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.profileDistribution", notNullValue()))
            .andExpect(jsonPath("$.userStatus", notNullValue()))
            .andExpect(jsonPath("$.auditSummary", notNullValue()))
            .andExpect(jsonPath("$.recentActivities", notNullValue()));
    }

    @Test
    @WithMockUser(username = "admin@duolingo.com", roles = {"ADMIN"})
    @DisplayName("Admin: Monitoramento de saúde e recursos do sistema (/api/admin/system/health)")
    void testGetSystemHealth() throws Exception {
        mockMvc.perform(get("/api/admin/system/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")))
            .andExpect(jsonPath("$.database.status", is("UP")))
            .andExpect(jsonPath("$.systemResources.availableProcessors", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.application.applicationName", is("Duolinfo Backend")));
    }

    @Test
    @WithMockUser(username = "admin@duolingo.com", roles = {"ADMIN"})
    @DisplayName("Admin: Consulta e filtro na Central de Logs e Auditoria (/api/admin/logs)")
    void testGetAuditLogs() throws Exception {
        // Grava evento de teste
        auditLogService.recordEvent(
            "SECURITY",
            "WARN",
            "TEST_AUDIT_ACTION",
            "security_tester@duolinfo.com",
            "ADMIN",
            "TestEntity",
            "100",
            "SUCCESS",
            "192.168.1.1",
            "corr-test-123",
            "Evento de teste de auditoria"
        );

        mockMvc.perform(get("/api/admin/logs")
                .param("category", "SECURITY")
                .param("severity", "WARN")
                .param("search", "TEST_AUDIT_ACTION"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", notNullValue()))
            .andExpect(jsonPath("$.content[0].action", is("TEST_AUDIT_ACTION")))
            .andExpect(jsonPath("$.content[0].category", is("SECURITY")))
            .andExpect(jsonPath("$.content[0].severity", is("WARN")));
    }

    @Test
    @WithMockUser(username = "admin@duolingo.com", roles = {"ADMIN"})
    @DisplayName("Admin: Suspender e reativar conta de usuário (/api/admin/users/{id}/toggle-active)")
    void testToggleUserActive() throws Exception {
        User user = new User();
        user.setEmail("suspend_me@example.com");
        user.setName("Suspend Target");
        user.setActive(true);
        user = userRepository.save(user);

        // Suspender
        mockMvc.perform(patch("/api/admin/users/" + user.getId() + "/toggle-active"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active", is(false)));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertFalse(updated.isActive());

        // Reativar
        mockMvc.perform(patch("/api/admin/users/" + user.getId() + "/toggle-active"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active", is(true)));

        User reactivated = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(reactivated.isActive());
    }

    @Test
    @WithMockUser(username = "admin@duolingo.com", roles = {"ADMIN"})
    @DisplayName("Admin: Gerenciamento completo de idiomas e catálogo (/api/admin/catalog)")
    void testCatalogManagementFlow() throws Exception {
        String payload = """
            {
                "catalogType": "LANGUAGE",
                "value": "Esperanto",
                "active": true
            }
        """;

        // Criação
        var result = mockMvc.perform(post("/api/admin/catalog")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value", is("Esperanto")))
            .andExpect(jsonPath("$.catalogType", is("LANGUAGE")))
            .andExpect(jsonPath("$.active", is(true)))
            .andReturn();

        String content = result.getResponse().getContentAsString();
        Long optionId = catalogOptionRepository.findByCatalogType("LANGUAGE").stream()
            .filter(o -> o.getValue().equals("Esperanto"))
            .map(CatalogOption::getId)
            .findFirst()
            .orElseThrow();

        // Alternância de status ativo
        mockMvc.perform(patch("/api/admin/catalog/" + optionId + "/toggle"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active", is(false)));

        // Listagem
        mockMvc.perform(get("/api/admin/catalog").param("type", "LANGUAGE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", notNullValue()));

        // Exclusão
        mockMvc.perform(delete("/api/admin/catalog/" + optionId))
            .andExpect(status().isNoContent());

        assertFalse(catalogOptionRepository.existsById(optionId));
    }

    @Test
    @WithMockUser(username = "admin@duolingo.com", roles = {"ADMIN"})
    @DisplayName("Admin: Listagem de documentos de professores (/api/admin/documents)")
    void testListTeacherDocuments() throws Exception {
        mockMvc.perform(get("/api/admin/documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @WithMockUser(username = "student@duolingo.com", roles = {"STUDENT"})
    @DisplayName("Segurança: Estudante comum não pode acessar endpoints administrativos")
    void testStudentForbiddenFromAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/overview"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/system/health"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/logs"))
            .andExpect(status().isForbidden());
    }
}
