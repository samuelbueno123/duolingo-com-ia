package com.duolinfo.ia.proj.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.duolinfo.ia.proj.dto.ai.ExplainMistakeRequestDTO;
import com.duolinfo.ia.proj.dto.ai.ExplainMistakeResponseDTO;
import com.duolinfo.ia.proj.util.SupportedLanguages;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiExplanationService {

    private final ObjectMapper objectMapper;
    private final String aiApiKey;
    private final String aiProviderUrl;
    private final String aiModel;
    private final HttpClient httpClient;

    public AiExplanationService(
            ObjectMapper objectMapper,
            @Value("${app.ai.api-key:}") String aiApiKey,
            @Value("${app.ai.provider-url:https://generativelanguage.googleapis.com/v1beta/models}") String aiProviderUrl,
            @Value("${app.ai.model:gemini-2.5-flash}") String aiModel) {
        this.objectMapper = objectMapper;
        this.aiApiKey = aiApiKey;
        this.aiProviderUrl = aiProviderUrl;
        this.aiModel = aiModel;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public ExplainMistakeResponseDTO explainMistake(ExplainMistakeRequestDTO request) {
        String lang = request.language() != null && !request.language().isBlank()
            ? SupportedLanguages.normalizeOrThrow(request.language())
            : "o idioma estudado";

        if (aiApiKey != null && !aiApiKey.isBlank() && !aiApiKey.startsWith("AQ.placeholder")) {
            try {
                String prompt = String.format(
                    "Você é um tutor pedagógico de idiomas especialista em %s. O estudante errou uma questão.\n"
                    + "Pergunta: \"%s\"\n"
                    + "Resposta incorreta do estudante: \"%s\"\n"
                    + "Resposta correta esperada: \"%s\"\n\n"
                    + "Responda em formato JSON estrito com os campos:\n"
                    + "{\n"
                    + "  \"explanation\": \"Explicação pedagógica clara e empática do erro em português\",\n"
                    + "  \"grammarRule\": \"Regra gramatical ou uso idiomático envolvido\",\n"
                    + "  \"memoryTip\": \"Dica mnemônica ou truque prático para não esquecer\",\n"
                    + "  \"similarExamples\": [\"Exemplo 1 no idioma estudado\", \"Exemplo 2\"]\n"
                    + "}\nNão adicione markdown em volta do JSON.",
                    lang, request.question(), request.wrongAnswer(), request.correctAnswer()
                );

                String model = (aiModel != null && !aiModel.isBlank()) ? aiModel.trim() : "gemini-2.5-flash";
                String endpoint = String.format("%s/%s:generateContent?key=%s", aiProviderUrl, model, aiApiKey);
                Map<String, Object> payload = Map.of(
                    "contents", List.of(
                        Map.of("parts", List.of(
                            Map.of("text", prompt)
                        ))
                    ),
                    "generationConfig", Map.of(
                        "temperature", 0.3,
                        "responseMimeType", "application/json"
                    )
                );

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray() && !candidates.isEmpty()) {
                        String rawJson = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                        JsonNode parsed = objectMapper.readTree(cleanMarkdownJson(rawJson));
                        String explanation = parsed.path("explanation").asText("");
                        String grammarRule = parsed.path("grammarRule").asText("");
                        String memoryTip = parsed.path("memoryTip").asText("");
                        List<String> examples = new java.util.ArrayList<>();
                        if (parsed.path("similarExamples").isArray()) {
                            parsed.path("similarExamples").forEach(item -> examples.add(item.asText()));
                        }
                        if (!explanation.isBlank()) {
                            return new ExplainMistakeResponseDTO(explanation, grammarRule, memoryTip, examples);
                        }
                    }
                }
            } catch (Exception ignored) {
                // Fallback to deterministic rule-based pedagogic guidance
            }
        }

        return generateFallbackExplanation(request, lang);
    }

    private ExplainMistakeResponseDTO generateFallbackExplanation(ExplainMistakeRequestDTO request, String language) {
        String explanation = String.format(
            "Em %s, a expressão correta para '%s' é '%s'. Sua resposta '%s' não se adequa à sintaxe ou ao vocabulário usual para este contexto.",
            language, request.question(), request.correctAnswer(), request.wrongAnswer()
        );
        String grammarRule = String.format("Atenção à concordância e estrutura de sentenças em %s.", language);
        String memoryTip = String.format("Associe '%s' ao significado chave em situações reais de conversa.", request.correctAnswer());
        List<String> examples = List.of(
            String.format("Exemplo: %s", request.correctAnswer()),
            "Pratique a frase em voz alta para fixar o ritmo e a pronúncia."
        );

        return new ExplainMistakeResponseDTO(explanation, grammarRule, memoryTip, examples);
    }

    private static String cleanMarkdownJson(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
