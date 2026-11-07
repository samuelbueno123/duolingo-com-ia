package com.duolinfo.ia.proj.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.ai.AiConversationMessageRequestDTO;
import com.duolinfo.ia.proj.dto.ai.AiConversationMessageResponseDTO;
import com.duolinfo.ia.proj.dto.ai.AiConversationSessionResponseDTO;
import com.duolinfo.ia.proj.dto.ai.AiConversationStartDTO;
import com.duolinfo.ia.proj.dto.ai.AiMistakeSummaryDTO;
import com.duolinfo.ia.proj.dto.ai.AiScenarioDTO;
import com.duolinfo.ia.proj.entity.AiConversationMessage;
import com.duolinfo.ia.proj.entity.AiConversationSession;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.repository.AiConversationMessageRepository;
import com.duolinfo.ia.proj.repository.AiConversationSessionRepository;
import com.duolinfo.ia.proj.repository.StudentLanguageRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiConversationService {

    private static final Logger log = LoggerFactory.getLogger(AiConversationService.class);

    private final AiConversationSessionRepository sessionRepository;
    private final AiConversationMessageRepository messageRepository;
    private final StudentRepository studentRepository;
    private final StudentLanguageRepository studentLanguageRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final String aiApiKey;
    private final String aiProviderUrl;
    private final String aiModel;

    private static final List<AiScenarioDTO> PRESET_SCENARIOS = List.of(
        new AiScenarioDTO(
            "airport_checkin",
            "Check-in no Aeroporto",
            "Pratique despachar malas, escolher assento e responder perguntas alfandegárias.",
            "Viagem",
            "A2",
            "Agente de Voo",
            "Passageiro",
            "Hello! Welcome to the airport check-in counter. May I see your ticket and passport, please?",
            List.of("Here is my passport", "Can I have a window seat?", "I have one bag to check")
        ),
        new AiScenarioDTO(
            "restaurant_order",
            "Jantar no Restaurante",
            "Faça seu pedido, pergunte sobre ingredientes e peça a conta educadamente.",
            "Restaurante",
            "B1",
            "Garçom Atencioso",
            "Cliente",
            "Good evening! Welcome to our bistro. Would you like to start with some drinks or hear today's chef specials?",
            List.of("What do you recommend?", "I would like the steak, please", "Could we get the bill, please?")
        ),
        new AiScenarioDTO(
            "job_interview",
            "Entrevista de Emprego",
            "Apresente suas experiências, qualidades e responda a perguntas situacionais.",
            "Carreira",
            "B2",
            "Entrevistador de Recursos Humanos",
            "Candidato",
            "Welcome! Thank you for joining us today. To begin, could you tell me a little bit about yourself and your professional background?",
            List.of("In my previous role...", "My biggest strength is...", "I am passionate about solving challenges")
        ),
        new AiScenarioDTO(
            "hotel_concierge",
            "Concierge e Turismo",
            "Peça recomendações turísticas, resolva problemas no quarto e agende transporte.",
            "Viagem",
            "B1",
            "Concierge do Hotel",
            "Hóspede",
            "Good day! How can I assist you with your stay today? Are you looking for local recommendations or assistance?",
            List.of("Could you recommend a good local museum?", "The air conditioner is not working", "Could you call a taxi for me?")
        ),
        new AiScenarioDTO(
            "coffee_shop",
            "Café da Manhã na Cafeteria",
            "Faça pedidos rápidos de bebidas, lanches e converse cordialmente.",
            "Cotidiano",
            "A1",
            "Barista",
            "Cliente",
            "Hi there! Welcome to the café. What can I get started for you today?",
            List.of("I'd like a medium cappuccino, please", "With oat milk, please", "How much is the croissant?")
        ),
        new AiScenarioDTO(
            "tech_presentation",
            "Apresentação de Ideia Inovadora",
            "Defenda sua proposta, responda a objeções e use vocabulário profissional avançado.",
            "Negócios",
            "C1",
            "Investidor Parceiro",
            "Empreendedor",
            "Thank you for the pitch summary. Looking at your market projections, what is your primary strategy to outpace existing incumbents?",
            List.of("Our core differentiator is...", "We have targeted high-retention segments", "Scalability is built into our core architecture")
        )
    );

    public AiConversationService(
            AiConversationSessionRepository sessionRepository,
            AiConversationMessageRepository messageRepository,
            StudentRepository studentRepository,
            StudentLanguageRepository studentLanguageRepository,
            ObjectMapper objectMapper,
            @Value("${app.ai.api-key:AQ.Ab8RN6IdHoAh6sEX4IdGe_WsMRxDWUtZiuiC5N5EAQNTkOkQkQ}") String aiApiKey,
            @Value("${app.ai.provider-url:https://generativelanguage.googleapis.com/v1beta/models}") String aiProviderUrl,
            @Value("${app.ai.model:gemini-3.5-flash-lite}") String aiModel) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.studentRepository = studentRepository;
        this.studentLanguageRepository = studentLanguageRepository;
        this.objectMapper = objectMapper;
        this.aiApiKey = aiApiKey;
        this.aiProviderUrl = aiProviderUrl;
        this.aiModel = aiModel;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public List<AiScenarioDTO> getScenarios(String cefrLevel) {
        if (cefrLevel == null || cefrLevel.isBlank()) {
            return PRESET_SCENARIOS;
        }
        return PRESET_SCENARIOS.stream()
                .filter(s -> s.cefrLevel().equalsIgnoreCase(cefrLevel.trim()))
                .toList();
    }

    @Transactional
    public AiConversationSessionResponseDTO startSession(Long studentId, AiConversationStartDTO request) {
        AiScenarioDTO scenario = PRESET_SCENARIOS.stream()
                .filter(s -> s.code().equalsIgnoreCase(request.scenarioCode()))
                .findFirst()
                .orElse(PRESET_SCENARIOS.get(0));

        String lang = request.targetLanguage() != null && !request.targetLanguage().isBlank()
                ? request.targetLanguage()
                : "Inglês";
        String level = request.cefrLevel() != null && !request.cefrLevel().isBlank()
                ? request.cefrLevel()
                : scenario.cefrLevel();

        AiConversationSession session = AiConversationSession.builder()
                .studentId(studentId)
                .scenarioCode(scenario.code())
                .scenarioTitle(scenario.title())
                .targetLanguage(lang)
                .cefrLevel(level)
                .personaRole(scenario.personaRole())
                .active(true)
                .totalTurns(0)
                .grammarCorrectionsCount(0)
                .startedAt(LocalDateTime.now())
                .build();

        session = sessionRepository.save(session);

        // Initial greeting message from AI persona
        String greetingText = adaptGreetingForLanguage(scenario.initialGreeting(), lang);
        AiConversationMessage initialAiMsg = AiConversationMessage.builder()
                .sessionId(session.getId())
                .sender("AI")
                .content(greetingText)
                .timestamp(LocalDateTime.now())
                .build();

        initialAiMsg = messageRepository.save(initialAiMsg);

        return AiConversationSessionResponseDTO.fromEntity(
            session,
            List.of(AiConversationMessageResponseDTO.fromEntity(initialAiMsg))
        );
    }

    @Transactional
    public AiConversationSessionResponseDTO sendMessage(Long studentId, Long sessionId, AiConversationMessageRequestDTO request) {
        AiConversationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão de conversação não encontrada."));

        if (!session.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para acessar esta conversa.");
        }

        if (!Boolean.TRUE.equals(session.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta sessão de conversação já foi encerrada.");
        }

        String userText = request.message().trim();
        List<AiConversationMessage> previousMessages = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);

        // Evaluate grammar and generate conversational continuation
        AiEvaluationAndReply aiResult = generateAiEvaluationAndReply(session, previousMessages, userText);

        AiConversationMessage userMsg = AiConversationMessage.builder()
                .sessionId(session.getId())
                .sender("USER")
                .content(userText)
                .audioUrl(request.audioUrl())
                .timestamp(LocalDateTime.now())
                .hasGrammarIssues(aiResult.hasGrammarIssues())
                .grammarCorrection(aiResult.grammarCorrection())
                .explanation(aiResult.explanation())
                .betterNativeAlternative(aiResult.betterNativeAlternative())
                .fluencyScore(aiResult.fluencyScore())
                .build();

        messageRepository.save(userMsg);

        // AI message reply
        AiConversationMessage aiMsg = AiConversationMessage.builder()
                .sessionId(session.getId())
                .sender("AI")
                .content(aiResult.aiReply())
                .timestamp(LocalDateTime.now().plusSeconds(1))
                .build();

        messageRepository.save(aiMsg);

        // Update session stats
        session.setTotalTurns(session.getTotalTurns() + 1);
        if (Boolean.TRUE.equals(aiResult.hasGrammarIssues())) {
            session.setGrammarCorrectionsCount(session.getGrammarCorrectionsCount() + 1);
        }
        sessionRepository.save(session);

        // Reward student with 10 XP per conversation turn
        awardExperience(studentId, session.getTargetLanguage(), 10);

        List<AiConversationMessage> updatedMessages = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        List<AiConversationMessageResponseDTO> messageDTOs = updatedMessages.stream()
                .map(AiConversationMessageResponseDTO::fromEntity)
                .toList();

        return AiConversationSessionResponseDTO.fromEntity(session, messageDTOs);
    }

    @Transactional(readOnly = true)
    public AiConversationSessionResponseDTO getSessionDetails(Long studentId, Long sessionId) {
        AiConversationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada."));

        if (!session.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não autorizado.");
        }

        List<AiConversationMessage> messages = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        return AiConversationSessionResponseDTO.fromEntity(
            session,
            messages.stream().map(AiConversationMessageResponseDTO::fromEntity).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<AiConversationSessionResponseDTO> getMySessions(Long studentId, String targetLanguage) {
        List<AiConversationSession> sessions = (targetLanguage != null && !targetLanguage.isBlank())
                ? sessionRepository.findByStudentIdAndTargetLanguageOrderByStartedAtDesc(studentId, targetLanguage)
                : sessionRepository.findByStudentIdOrderByStartedAtDesc(studentId);

        return sessions.stream()
                .map(s -> AiConversationSessionResponseDTO.fromEntity(s, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AiMistakeSummaryDTO> getMyMistakes(Long studentId) {
        List<AiConversationMessage> mistakeMsgs = messageRepository.findGrammarIssuesByStudentId(studentId);
        return mistakeMsgs.stream()
                .map(m -> new AiMistakeSummaryDTO(
                    m.getId(),
                    m.getSessionId(),
                    m.getContent(),
                    m.getGrammarCorrection(),
                    m.getExplanation(),
                    m.getBetterNativeAlternative(),
                    m.getTimestamp()
                ))
                .toList();
    }

    @Transactional
    public AiConversationSessionResponseDTO endSession(Long studentId, Long sessionId) {
        AiConversationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada."));

        if (!session.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não autorizado.");
        }

        session.setActive(false);
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);

        List<AiConversationMessage> messages = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        return AiConversationSessionResponseDTO.fromEntity(
            session,
            messages.stream().map(AiConversationMessageResponseDTO::fromEntity).toList()
        );
    }

    private void awardExperience(Long studentId, String language, int xpAmount) {
        try {
            studentRepository.findById(studentId).ifPresent(student -> {
                StudentLanguage sl = studentLanguageRepository
                        .findByStudentAndLanguageNameIgnoreCase(student, language)
                        .orElse(null);
                if (sl != null) {
                    sl.setScore((sl.getScore() != null ? sl.getScore() : 0) + xpAmount);
                    studentLanguageRepository.save(sl);
                }
            });
        } catch (Exception e) {
            log.warn("Erro ao atribuir XP de conversação IA: {}", e.getMessage());
        }
    }

    private record AiEvaluationAndReply(
        boolean hasGrammarIssues,
        String grammarCorrection,
        String explanation,
        String betterNativeAlternative,
        double fluencyScore,
        String aiReply
    ) {}

    private AiEvaluationAndReply generateAiEvaluationAndReply(
            AiConversationSession session,
            List<AiConversationMessage> history,
            String userText) {

        if (aiApiKey != null && !aiApiKey.isBlank()) {
            try {
                return callAiProvider(session, history, userText);
            } catch (Exception e) {
                log.warn("Chamada Gemini falhou na conversação, acionando fallback inteligente: {}", e.getMessage());
            }
        }

        return fallbackIntelligentEngine(session, userText);
    }

    private AiEvaluationAndReply callAiProvider(
            AiConversationSession session,
            List<AiConversationMessage> history,
            String userText) throws Exception {

        String baseUrl = aiProviderUrl != null ? aiProviderUrl.trim() : "";
        String endpoint = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String model = (aiModel != null && !aiModel.isBlank()) ? aiModel.trim() : "gemini-3.5-flash-lite";
        String url = endpoint + "/" + model + ":generateContent?key=" + aiApiKey.trim();

        StringBuilder conversationContext = new StringBuilder();
        int startIndex = Math.max(0, history.size() - 6);
        for (int i = startIndex; i < history.size(); i++) {
            AiConversationMessage msg = history.get(i);
            conversationContext.append(msg.getSender()).append(": ").append(msg.getContent()).append("\n");
        }

        String prompt = """
            Você é um assistente pedagógico de idiomas de alto nível em uma simulação de conversação interativa.
            Cenário: %s (Nível CEFR: %s, Idioma: %s).
            Seu Papel: %s.
            Histórico recente:
            %s
            O aluno acabou de dizer: "%s"

            Sua tarefa é retornar EXCLUSIVAMENTE um objeto JSON com esta estrutura exata:
            {
              "hasGrammarIssues": true/false,
              "grammarCorrection": "frase corrigida do aluno (ou null se perfeita)",
              "explanation": "explicação pedagógica clara e breve em português do porquê foi corrigido (ou elogio se correto)",
              "betterNativeAlternative": "sugestão de como um nativo fluente diria a mesma ideia de forma mais idiomática",
              "fluencyScore": número entre 0 e 100,
              "aiReply": "sua resposta em personagem no idioma %s, continuando a conversa e fazendo uma pergunta relevante para estimular nova fala"
            }
            """.formatted(
                session.getScenarioTitle(),
                session.getCefrLevel(),
                session.getTargetLanguage(),
                session.getPersonaRole(),
                conversationContext.toString(),
                userText,
                session.getTargetLanguage()
            );

        Map<String, Object> payload = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
            ),
            "generationConfig", Map.of(
                "temperature", 0.5,
                "responseMimeType", "application/json"
            )
        );

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            String clean = text.replace("```json", "").replace("```", "").trim();
            JsonNode json = objectMapper.readTree(clean);

            boolean hasIssues = json.path("hasGrammarIssues").asBoolean(false);
            String correction = json.path("grammarCorrection").isNull() ? null : json.path("grammarCorrection").asText(null);
            String explanation = json.path("explanation").asText("Excelente participação.");
            String nativeAlt = json.path("betterNativeAlternative").asText(null);
            double fluency = json.path("fluencyScore").asDouble(90.0);
            String reply = json.path("aiReply").asText("I understand. Could you tell me more about that?");

            return new AiEvaluationAndReply(hasIssues, correction, explanation, nativeAlt, fluency, reply);
        }

        throw new IllegalStateException("Gemini returned HTTP " + response.statusCode());
    }

    private AiEvaluationAndReply fallbackIntelligentEngine(AiConversationSession session, String userText) {
        String lower = userText.toLowerCase(Locale.ROOT).trim();
        boolean hasIssues = false;
        String correction = null;
        String explanation = "Muito bom! Sua mensagem comunica a ideia com clareza.";
        String nativeAlt = userText;
        double fluency = 88.0;

        // Heuristic pedagogical checks for typical language learners
        if (session.getTargetLanguage().equalsIgnoreCase("Inglês")) {
            if (lower.contains("i have ") && (lower.contains(" years") || lower.contains(" years old"))) {
                hasIssues = true;
                correction = userText.replaceAll("(?i)\\bi have (\\d+) years\\b", "I am $1 years old");
                explanation = "Em inglês, usamos o verbo 'to be' (I am) para indicar idade, e não 'to have'.";
                nativeAlt = "I'm " + (lower.contains("2") ? "25" : "twenty") + " years old.";
                fluency = 70.0;
            } else if (lower.contains("he don't") || lower.contains("she don't") || lower.contains("it don't")) {
                hasIssues = true;
                correction = userText.replaceAll("(?i)\\b(he|she|it) don't\\b", "$1 doesn't");
                explanation = "Para a terceira pessoa do singular (he, she, it), a forma negativa correta no presente simples é 'doesn't'.";
                nativeAlt = userText.replaceAll("(?i)\\bdon't\\b", "doesn't");
                fluency = 65.0;
            } else if (userText.length() > 0 && Character.isLowerCase(userText.charAt(0))) {
                hasIssues = true;
                correction = Character.toUpperCase(userText.charAt(0)) + userText.substring(1);
                explanation = "Lembre-se de sempre iniciar suas frases com letra maiúscula.";
                nativeAlt = correction;
                fluency = 82.0;
            } else if (!userText.endsWith(".") && !userText.endsWith("?") && !userText.endsWith("!")) {
                correction = userText + ".";
                explanation = "Excelente resposta! Lembre-se apenas de pontuar ao final para manter a precisão gramatical.";
                nativeAlt = userText + ".";
                fluency = 85.0;
            }
        } else if (session.getTargetLanguage().equalsIgnoreCase("Espanhol")) {
            if (lower.contains("yo soy ") && lower.contains("años")) {
                hasIssues = true;
                correction = userText.replaceAll("(?i)\\byo soy (\\d+) años\\b", "Yo tengo $1 años");
                explanation = "Em espanhol, para indicar idade, usa-se o verbo 'tener' (tengo ... años).";
                nativeAlt = "Tengo años.";
                fluency = 72.0;
            }
        }

        // Generate contextual in-character AI response
        String reply = generateInCharacterReply(session.getScenarioCode(), session.getTargetLanguage(), userText);

        return new AiEvaluationAndReply(hasIssues, correction, explanation, nativeAlt, fluency, reply);
    }

    private String generateInCharacterReply(String scenarioCode, String language, String userText) {
        boolean isEnglish = language == null || language.equalsIgnoreCase("Inglês");

        if (isEnglish) {
            return switch (scenarioCode) {
                case "airport_checkin" ->
                    "Thank you very much. I have confirmed your reservation. Would you prefer an aisle seat or a window seat for your flight?";
                case "restaurant_order" ->
                    "That is an excellent choice! Would you like any side dishes or wine pairing with that order?";
                case "job_interview" ->
                    "That is very interesting experience. Could you give me a specific example of how you resolved a tough problem under a tight deadline?";
                case "hotel_concierge" ->
                    "Certainly, I would be delighted to assist you with that. Would you like a reservation made for tonight or tomorrow morning?";
                case "coffee_shop" ->
                    "Coming right up! Would you like whole milk, skim milk, or a plant-based alternative such as oat or almond milk?";
                case "tech_presentation" ->
                    "Fascinating perspective on the architecture. How do you plan to handle enterprise data governance and customer onboarding at scale?";
                default ->
                    "Thank you for sharing that! Could you elaborate on what you mean and how you would approach the next step?";
            };
        } else {
            return switch (scenarioCode) {
                case "airport_checkin" ->
                    "Muchas gracias. He verificado su reserva. ¿Prefiere asiento de pasillo o de ventanilla?";
                case "restaurant_order" ->
                    "¡Excelente elección! ¿Desea alguna bebida especial o guarnición para acompañar su plato?";
                default ->
                    "¡Entendido perfectamente! ¿Podrías contarme un poco más sobre eso para continuar?";
            };
        }
    }

    private String adaptGreetingForLanguage(String greeting, String targetLanguage) {
        if (targetLanguage.equalsIgnoreCase("Espanhol")) {
            return "¡Hola! Bienvenido. ¿En qué puedo ayudarte el día de hoy?";
        } else if (targetLanguage.equalsIgnoreCase("Francês")) {
            return "Bonjour et bienvenue ! Comment puis-je vous aider aujourd'hui ?";
        }
        return greeting;
    }
}
