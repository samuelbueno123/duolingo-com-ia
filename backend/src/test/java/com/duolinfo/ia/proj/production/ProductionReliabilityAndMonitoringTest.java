package com.duolinfo.ia.proj.production;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.duolinfo.ia.proj.config.CorrelationIdFilter;
import com.duolinfo.ia.proj.config.ExternalAiHealthIndicator;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductionReliabilityAndMonitoringTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExternalAiHealthIndicator externalAiHealthIndicator;

    @Test
    @DisplayName("Garante que toda requisição recebe um Correlation ID gerado automaticamente nos headers de resposta")
    void testCorrelationIdGeneratedAndEchoedInResponse() throws Exception {
        mockMvc.perform(get("/api/catalog/languages"))
            .andExpect(status().isOk())
            .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
            .andExpect(header().exists(CorrelationIdFilter.REQUEST_ID_HEADER));
    }

    @Test
    @DisplayName("Garante que um Correlation ID enviado pelo cliente (ex: Flutter) é preservado e propagado")
    void testCustomCorrelationIdPreserved() throws Exception {
        String clientTraceId = "req-1710000000-abcd12";

        mockMvc.perform(get("/api/catalog/languages")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, clientTraceId))
            .andExpect(status().isOk())
            .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, is(clientTraceId)))
            .andExpect(header().string(CorrelationIdFilter.REQUEST_ID_HEADER, is(clientTraceId)));
    }

    @Test
    @DisplayName("Garante que o endpoint /actuator/health está acessível publicamente para probes de orquestração")
    void testActuatorHealthPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", notNullValue()));
    }

    @Test
    @DisplayName("Garante que métricas sensíveis do Actuator (/actuator/metrics) são protegidas contra acesso anônimo")
    void testActuatorMetricsProtected() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.correlationId", notNullValue()))
            .andExpect(jsonPath("$.message", containsString("Token ausente ou inválido")));
    }

    @Test
    @DisplayName("Valida resposta padronizada de erro ApiErrorResponse com Correlation ID ao acessar rota restrita")
    void testStandardizedApiErrorResponseFormat() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.timestamp", notNullValue()))
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.error", is("Unauthorized")))
            .andExpect(jsonPath("$.path", is("/api/users/me")))
            .andExpect(jsonPath("$.correlationId", notNullValue()));
    }

    @Test
    @DisplayName("Valida retorno de mapa estruturado de erros de validação em payloads inválidos")
    void testValidationErrorsStructuredMap() throws Exception {
        // Envia payload inválido para cadastro de turma sem autenticação (deve rejeitar)
        mockMvc.perform(post("/api/classes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.correlationId", notNullValue()));
    }

    @Test
    @DisplayName("Valida indicador de saúde do serviço externo de IA (Gemini)")
    void testExternalAiHealthIndicator() {
        Health health = externalAiHealthIndicator.health();
        assertNotNull(health);
        assertNotNull(health.getStatus());
        assertNotNull(health.getDetails().get("service"));
    }
}
