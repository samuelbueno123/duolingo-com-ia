package com.duolinfo.ia.proj.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Indicador de saúde do serviço externo de Inteligência Artificial (Gemini).
 * Informa o status de configuração e conectividade no endpoint /actuator/health.
 */
@Component
public class ExternalAiHealthIndicator implements HealthIndicator {

    private final String aiApiKey;
    private final String aiProviderUrl;
    private final String aiModel;

    public ExternalAiHealthIndicator(
            @Value("${app.ai.api-key:}") String aiApiKey,
            @Value("${app.ai.provider-url:}") String aiProviderUrl,
            @Value("${app.ai.model:gemini-2.5-flash}") String aiModel) {
        this.aiApiKey = aiApiKey;
        this.aiProviderUrl = aiProviderUrl;
        this.aiModel = aiModel;
    }

    @Override
    public Health health() {
        if (aiApiKey == null || aiApiKey.isBlank() || aiApiKey.startsWith("AQ.placeholder")) {
            return Health.unknown()
                .withDetail("service", "External AI")
                .withDetail("configured", false)
                .withDetail("message", "Chave AI_API_KEY não informada ou padrão")
                .build();
        }

        return Health.up()
            .withDetail("service", "Google Gemini AI")
            .withDetail("configured", true)
            .withDetail("model", aiModel)
            .withDetail("providerUrl", aiProviderUrl != null && !aiProviderUrl.isBlank() ? aiProviderUrl : "default")
            .build();
    }
}
