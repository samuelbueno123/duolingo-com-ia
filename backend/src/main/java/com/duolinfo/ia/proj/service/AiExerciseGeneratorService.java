package com.duolinfo.ia.proj.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.AiExerciseGenerateRequestDTO;
import com.duolinfo.ia.proj.dto.request.ExerciseQuestionRequestDTO;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiExerciseGeneratorService {

    private final ObjectMapper objectMapper;
    private final String aiApiKey;
    private final String aiProviderUrl;
    private final String aiModel;
    private final HttpClient httpClient;

    public AiExerciseGeneratorService(
            ObjectMapper objectMapper,
            @Value("${app.ai.api-key:}") String aiApiKey,
            @Value("${app.ai.provider-url:https://generativelanguage.googleapis.com/v1beta/models}") String aiProviderUrl,
            @Value("${app.ai.model:gemini-2.5-flash}") String aiModel) {
        this.objectMapper = objectMapper;
        this.aiApiKey = aiApiKey;
        this.aiProviderUrl = aiProviderUrl;
        this.aiModel = aiModel;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public boolean isConfigured() {
        return aiApiKey != null && !aiApiKey.isBlank();
    }

    public List<ExerciseQuestionRequestDTO> generateQuestions(AiExerciseGenerateRequestDTO request) {
        if (!isConfigured()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "A geração com Inteligência Artificial não está ativa neste ambiente porque a credencial AI_API_KEY não foi configurada no servidor."
            );
        }

        String promptOrInstructions = request != null && request.additionalInstructions() != null
                && !request.additionalInstructions().isBlank()
                ? request.additionalInstructions().trim()
                : (request != null && request.theme() != null ? request.theme().trim() : "");

        if (request == null || promptOrInstructions.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Descreva o tema ou instrução para gerar as questões com IA."
            );
        }

        int count = request.questionCount() > 0
                ? Math.max(1, Math.min(10, request.questionCount()))
                : 3;
        String language = request.languageName() != null && !request.languageName().isBlank()
                ? request.languageName().trim()
                : "Inglês";
        String level = request.proficiencyLevel() != null && !request.proficiencyLevel().isBlank()
                ? request.proficiencyLevel().trim()
                : "Fácil";
        String category = request.category() != null && !request.category().isBlank()
                ? request.category().trim()
                : "Gramática";
        String theme = request.theme() != null && !request.theme().isBlank()
                ? request.theme().trim()
                : "Geral";

        String stylesInstruction = request.questionStyles() != null && !request.questionStyles().isEmpty()
                ? "Priorize os seguintes formatos de questão: " + String.join(", ", request.questionStyles()) + "."
                : "Varie os formatos entre Objetiva, Verdadeiro ou Falso, Completar lacuna e Resposta aberta.";

        String systemPrompt = """
                Você é um especialista pedagógico de idiomas criando questões revisáveis para professores na plataforma Duolinfo.
                Retorne EXCLUSIVAMENTE um array JSON válido (sem markdown ```json) com %d questões para o idioma "%s", nível "%s", categoria "%s" e tema "%s".
                %s
                Cada item do array JSON deve conter exatamente estas propriedades:
                - "style": um dentre ["Objetiva", "Verdadeiro ou Falso", "Completar lacuna", "Ordenar frase", "Áudio", "Resposta aberta"]
                - "question": enunciado claro da questão
                - "options": lista com 4 alternativas para "Objetiva", ["Verdadeiro", "Falso"] para "Verdadeiro ou Falso", ou [] para os demais estilos
                - "correctAnswer": resposta exata correta (para "Objetiva" ou "Verdadeiro ou Falso")
                - "expectedAnswer": resposta esperada ou critério de avaliação (para "Completar lacuna", "Ordenar frase", "Áudio" ou "Resposta aberta")
                Instrução do professor: %s
                """.formatted(count, language, level, category, theme, stylesInstruction, promptOrInstructions);

        try {
            String rawText = callProvider(systemPrompt);
            return parseQuestionsJson(rawText);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Falha ao comunicar com o provedor de IA configurado: " + e.getMessage()
            );
        }
    }

    private String callProvider(String promptText) throws Exception {
        String baseUrl = aiProviderUrl != null ? aiProviderUrl.trim() : "";
        boolean isGemini = baseUrl.contains("generativelanguage.googleapis.com");

        HttpRequest httpRequest;
        if (isGemini) {
            String endpoint = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String url = endpoint + "/" + aiModel.trim() + ":generateContent?key=" + aiApiKey.trim();
            Map<String, Object> payload = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(Map.of("text", promptText)))
                ),
                "generationConfig", Map.of(
                    "temperature", 0.4,
                    "responseMimeType", "application/json"
                )
            );
            httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
        } else {
            Map<String, Object> payload = Map.of(
                "model", aiModel.trim(),
                "temperature", 0.4,
                "messages", List.of(
                    Map.of("role", "user", "content", promptText)
                )
            );
            httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiApiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
        }

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "O provedor de IA retornou status HTTP " + response.statusCode() + "."
            );
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (isGemini) {
            JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "O provedor de IA retornou uma resposta vazia.");
            }
            return textNode.asText();
        } else {
            JsonNode textNode = root.at("/choices/0/message/content");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "O provedor de IA retornou uma resposta vazia.");
            }
            return textNode.asText();
        }
    }

    public List<ExerciseQuestionRequestDTO> parseQuestionsJson(String rawText) {
        String cleaned = rawText == null ? "" : rawText.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline > 0) {
                cleaned = cleaned.substring(firstNewline + 1);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
            }
        }
        try {
            List<ExerciseQuestionRequestDTO> parsed = objectMapper.readValue(
                    cleaned,
                    new TypeReference<List<ExerciseQuestionRequestDTO>>() {}
            );
            List<ExerciseQuestionRequestDTO> normalized = new ArrayList<>();
            for (ExerciseQuestionRequestDTO item : parsed) {
                if (item.question() == null || item.question().isBlank()) {
                    continue;
                }
                String style = item.style() != null && !item.style().isBlank() ? item.style().trim() : "Objetiva";
                List<String> options = item.options() != null ? item.options() : List.of();
                String correct = item.correctAnswer();
                String expected = item.expectedAnswer();
                if (expected == null || expected.isBlank()) {
                    expected = correct;
                }
                if (correct == null || correct.isBlank()) {
                    correct = expected;
                }
                normalized.add(new ExerciseQuestionRequestDTO(
                        style,
                        item.question().trim(),
                        options,
                        correct,
                        expected
                ));
            }
            if (normalized.isEmpty()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "A resposta do provedor de IA não continha questões válidas."
                );
            }
            return normalized;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Não foi possível interpretar o JSON retornado pelo provedor de IA."
            );
        }
    }
}
