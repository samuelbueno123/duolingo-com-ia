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
import com.duolinfo.ia.proj.util.SupportedLanguages;

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
            @Value("${app.ai.api-key:}") String aiApiKey,
            @Value("${app.ai.provider-url:https://generativelanguage.googleapis.com/v1beta/models}") String aiProviderUrl,
            @Value("${app.ai.model:gemini-2.5-flash}") String aiModel) {
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
        return getScenarios(cefrLevel, SupportedLanguages.DEFAULT);
    }

    public List<AiScenarioDTO> getScenarios(String cefrLevel, String language) {
        String lang = SupportedLanguages.normalizeOrDefault(language, SupportedLanguages.DEFAULT);
        return PRESET_SCENARIOS.stream()
                .filter(s -> cefrLevel == null || cefrLevel.isBlank() || s.cefrLevel().equalsIgnoreCase(cefrLevel.trim()))
                .map(s -> localizeScenario(s, lang))
                .toList();
    }

    private AiScenarioDTO localizeScenario(AiScenarioDTO base, String lang) {
        if ("Inglês".equalsIgnoreCase(lang)) {
            return base;
        }
        String localizedGreeting = adaptGreetingForScenarioAndLanguage(base.code(), base.initialGreeting(), lang);
        List<String> localizedTargets = localizedTargetPhrases(base.code(), lang, base.targetPhrases());
        return new AiScenarioDTO(
            base.code(),
            base.title(),
            base.description(),
            base.category(),
            base.cefrLevel(),
            base.personaRole(),
            base.userRole(),
            localizedGreeting,
            localizedTargets
        );
    }

    @Transactional
    public AiConversationSessionResponseDTO startSession(Long studentId, AiConversationStartDTO request) {
        AiScenarioDTO scenario = PRESET_SCENARIOS.stream()
                .filter(s -> s.code().equalsIgnoreCase(request.scenarioCode()))
                .findFirst()
                .orElse(PRESET_SCENARIOS.get(0));

        String lang = SupportedLanguages.normalizeOrDefault(request.targetLanguage(), SupportedLanguages.DEFAULT);
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
        String greetingText = adaptGreetingForScenarioAndLanguage(scenario.code(), scenario.initialGreeting(), lang);
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

        if (aiApiKey != null && !aiApiKey.isBlank() && !aiApiKey.startsWith("AQ.placeholder")) {
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
        String model = (aiModel != null && !aiModel.isBlank()) ? aiModel.trim() : "gemini-2.5-flash";
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

        // Heuristic pedagogical checks across all 6 supported languages
        String targetLang = SupportedLanguages.normalizeOrDefault(session.getTargetLanguage(), SupportedLanguages.DEFAULT);
        if ("Inglês".equals(targetLang)) {
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
        } else if ("Espanhol".equals(targetLang)) {
            if (lower.matches(".*\\byo soy\\s+\\d+\\s+a[ñn]os.*")) {
                hasIssues = true;
                correction = userText.replaceAll("(?i)\\byo soy (\\d+) a[ñn]os\\b", "Yo tengo $1 años");
                explanation = "Em espanhol, para indicar idade, usa-se o verbo 'tener' (Yo tengo ... años).";
                nativeAlt = "Tengo esa edad.";
                fluency = 72.0;
            } else if (lower.contains("yo gusto ")) {
                hasIssues = true;
                correction = userText.replaceAll("(?i)\\byo gusto\\b", "A mí me gusta");
                explanation = "Em espanhol, 'gustar' concorda com o objeto: use 'Me gusta...' (singular) ou 'Me gustan...' (plural).";
                nativeAlt = "Me encantaría probar eso, por favor.";
                fluency = 74.0;
            } else if (userText.length() > 0 && Character.isLowerCase(userText.charAt(0))) {
                hasIssues = true;
                correction = Character.toUpperCase(userText.charAt(0)) + userText.substring(1);
                explanation = "Inicie sempre a frase em espanhol com letra maiúscula.";
                nativeAlt = correction;
                fluency = 83.0;
            }
        } else if ("Francês".equals(targetLang)) {
            if (lower.matches(".*\\bje suis\\s+\\d+\\s+ans.*")) {
                hasIssues = true;
                correction = userText.replaceAll("(?i)\\bje suis (\\d+) ans\\b", "J'ai $1 ans");
                explanation = "Em francês, expressamos idade com o verbo 'avoir' ('J'ai ... ans'), nunca 'être'.";
                nativeAlt = "J'ai l'âge indiqué.";
                fluency = 72.0;
            } else if (lower.contains("je veux ") && !lower.contains("s'il")) {
                hasIssues = true;
                correction = userText.replaceAll("(?i)\\bje veux\\b", "Je voudrais");
                explanation = "Em pedidos educados em francês (lojas, cafés, aeroportos), prefira 'Je voudrais...' (Eu gostaria) a 'Je veux'.";
                nativeAlt = correction + ", s'il vous plaît.";
                fluency = 78.0;
            } else if (userText.length() > 0 && Character.isLowerCase(userText.charAt(0))) {
                hasIssues = true;
                correction = Character.toUpperCase(userText.charAt(0)) + userText.substring(1);
                explanation = "Comece a frase com letra maiúscula e atenha-se aos acentos em francês.";
                nativeAlt = correction;
                fluency = 83.0;
            }
        } else if ("Alemão".equals(targetLang)) {
            if (lower.contains("ich möchte ") || lower.contains("ich moechte ")) {
                explanation = "Ótimo uso de 'ich möchte' (eu gostaria)! Lembre-se de colocar infinitivos ao final da frase se houver outro verbo.";
                nativeAlt = userText + " bitte.";
                fluency = 91.0;
            } else if (lower.contains("ich bin wollen") || lower.contains("ich habe wollen")) {
                hasIssues = true;
                correction = "Ich möchte ...";
                explanation = "Em alemão, use diretamente o verbo modal conjugado ('Ich möchte' ou 'Ich will') sem auxiliar 'bin/habe'.";
                nativeAlt = "Ich hätte gern..., bitte.";
                fluency = 72.0;
            } else if (userText.length() > 0 && Character.isLowerCase(userText.charAt(0))) {
                hasIssues = true;
                correction = Character.toUpperCase(userText.charAt(0)) + userText.substring(1);
                explanation = "Em alemão, a primeira palavra da frase e TODOS os substantivos iniciam com letra maiúscula.";
                nativeAlt = correction;
                fluency = 80.0;
            }
        } else if ("Italiano".equals(targetLang)) {
            if (lower.matches(".*\\bio sono\\s+\\d+\\s+anni.*")) {
                hasIssues = true;
                correction = userText.replaceAll("(?i)\\bio sono (\\d+) anni\\b", "Io ho $1 anni");
                explanation = "Em italiano, usamos 'avere' para idade ('Ho ... anni'), e não 'essere'.";
                nativeAlt = "Ho l'età indicata.";
                fluency = 72.0;
            } else if (lower.contains("io voglio ")) {
                hasIssues = true;
                correction = userText.replaceAll("(?i)\\bio voglio\\b", "Vorrei");
                explanation = "Para soar educado em italiano ao fazer pedidos, substitua 'voglio' por 'Vorrei...' (Eu gostaria).";
                nativeAlt = correction + ", per favore.";
                fluency = 78.0;
            } else if (userText.length() > 0 && Character.isLowerCase(userText.charAt(0))) {
                hasIssues = true;
                correction = Character.toUpperCase(userText.charAt(0)) + userText.substring(1);
                explanation = "Inicie sempre a sentença em italiano com letra maiúscula.";
                nativeAlt = correction;
                fluency = 83.0;
            }
        } else if ("Japonês".equals(targetLang)) {
            if (lower.contains("desu") || userText.contains("です") || userText.contains("ます") || lower.contains("onegaishimasu") || userText.contains("お願いします")) {
                explanation = "Excelente registro polido (-desu/-masu ou onegaishimasu)! Isso soa natural e respeitoso em japonês.";
                nativeAlt = userText;
                fluency = 92.0;
            } else if (!lower.isEmpty() && !lower.contains("desu") && !lower.contains("masu") && !userText.contains("。") && userText.matches("^[a-zA-Z\\s']+$")) {
                explanation = "Dica de japonês: em conversas com atendentes ou entrevistas, finalize frases romaji/kana com formas polidas como 'desu' ou 'onegaishimasu'.";
                nativeAlt = userText + ", onegaishimasu.";
                fluency = 82.0;
            }
        }

        // Generate contextual in-character AI response
        String reply = generateInCharacterReply(session.getScenarioCode(), targetLang, userText);

        return new AiEvaluationAndReply(hasIssues, correction, explanation, nativeAlt, fluency, reply);
    }

    private String generateInCharacterReply(String scenarioCode, String language, String userText) {
        String lang = SupportedLanguages.normalizeOrDefault(language, SupportedLanguages.DEFAULT);
        return switch (lang) {
            case "Espanhol" -> switch (scenarioCode) {
                case "airport_checkin" -> "Muchas gracias. He confirmado su reserva. ¿Prefiere asiento de pasillo o de ventanilla?";
                case "restaurant_order" -> "¡Excelente elección! ¿Le gustaría alguna bebida o guarnición para acompañar su plato?";
                case "job_interview" -> "Qué experiencia tan interesante. ¿Podría darme un ejemplo concreto de cómo resolvió un reto difícil?";
                case "hotel_concierge" -> "Por supuesto, con mucho gusto le ayudo con eso. ¿Desea que reserve para hoy por la noche?";
                case "coffee_shop" -> "¡Enseguida! ¿Prefiere su café con leche entera, descremada o bebida de avena?";
                case "tech_presentation" -> "Excelente enfoque tecnológico. ¿Cómo planean escalar la solución en nuevos mercados?";
                default -> "¡Perfecto! Cuénteme un poco más para continuar nuestra conversación.";
            };
            case "Francês" -> switch (scenarioCode) {
                case "airport_checkin" -> "Merci beaucoup. Votre billet est enregistré. Préférez-vous un siège côté fenêtre ou couloir ?";
                case "restaurant_order" -> "Très bon choix ! Souhaitez-vous une boisson ou une entrée pour commencer votre dîner ?";
                case "job_interview" -> "Votre parcours est très intéressant. Comment gérez-vous généralement les priorités sous pression ?";
                case "hotel_concierge" -> "Bien entendu, je m'en occupe tout de suite. Préférez-vous une visite culturelle ou gastronomique ?";
                case "coffee_shop" -> "C'est noté ! Le souhaitez-vous chaud ou glacé, et avec un croissant tout frais ?";
                case "tech_presentation" -> "Perspective solide ! Quelle est votre stratégie d'acquisition pour les six prochains mois ?";
                default -> "Très bien ! Pouvez-vous m'en dire un peu plus à ce sujet ?";
            };
            case "Alemão" -> switch (scenarioCode) {
                case "airport_checkin" -> "Vielen Dank! Ich habe Ihre Buchung gefunden. Möchten Sie lieber einen Fensterplatz oder einen Gangplatz?";
                case "restaurant_order" -> "Eine ausgezeichnete Wahl! Möchten Sie dazu ein Getränk oder die Tagesempfehlung des Kochs?";
                case "job_interview" -> "Das klingt nach wertvoller Erfahrung. Können Sie ein konkretes Projekt beschreiben, auf das Sie stolz sind?";
                case "hotel_concierge" -> "Sehr gerne helfe ich Ihnen dabei. Soll ich für heute Abend einen Tisch oder ein Taxi reservieren?";
                case "coffee_shop" -> "Kommt sofort! Möchten Sie normale Milch oder Hafermilch zu Ihrem Kaffee?";
                case "tech_presentation" -> "Überzeugendes Konzept! Wie stellen Sie die Skalierbarkeit und Datensicherheit bei großen Kunden sicher?";
                default -> "Verstehe! Erzählen Sie mir bitte noch etwas mehr darüber.";
            };
            case "Italiano" -> switch (scenarioCode) {
                case "airport_checkin" -> "Grazie mille! Ho confermato il suo biglietto. Preferisce un posto vicino al finestrino o al corridoio?";
                case "restaurant_order" -> "Ottima scelta! Desidera qualcosa da bere o un antipasto in attesa del piatto principale?";
                case "job_interview" -> "Esperienza davvero valida. Come affronta di solito le sfide lavorative in team?";
                case "hotel_concierge" -> "Certamente, è un piacere aiutarla! Desidera consigli sui musei vicini o preferisce prenotare un taxi?";
                case "coffee_shop" -> "Subito pronto! Lo preferisce caldo, tiepido o accompagnato da un cornetto alla crema?";
                case "tech_presentation" -> "Ottima visione! Qual è il principale vantaggio competitivo rispetto ai concorrenti attuali?";
                default -> "Perfetto! Mi racconti qualcosa in più per continuare.";
            };
            case "Japonês" -> switch (scenarioCode) {
                case "airport_checkin" -> "Arigatou gozaimasu! Yoyaku wo kakunin shimashita. Madogawa to tsurogawa, dochira ga ii desu ka? (窓側と通路側、どちらがいいですか？)";
                case "restaurant_order" -> "Subarashii erabi desu ne! Onomimono wa ikaga desu ka? (お飲み物はいかがですか？)";
                case "job_interview" -> "Totemo ii keiken desu ne. Chīmu de hataraku toki, nani wo ichiban taisetsu ni shite imasu ka? (何を一番大切にしていますか？)";
                case "hotel_concierge" -> "Kashikomarimashita! Kyō no goyotei ni awasete, chikaku no osusume supotto wo goshōkai shimashō ka? (ご紹介しましょうか？)";
                case "coffee_shop" -> "Shōshō omachi kudasai! Hotto to aisu, dochira ni nasaimasu ka? (ホットとアイス、どちらになさいますか？)";
                case "tech_presentation" -> "Totemo omoshiroi teian desu! Kongo no shijō senryaku ni tsuite motto oshiete kudasai. (もっと教えてください。)";
                default -> "Wakarimashita! Soshite, tsugi wa dō shitai desu ka? (次はどうしたいですか？)";
            };
            default -> switch (scenarioCode) {
                case "airport_checkin" -> "Thank you very much. I have confirmed your reservation. Would you prefer an aisle seat or a window seat for your flight?";
                case "restaurant_order" -> "That is an excellent choice! Would you like any side dishes or wine pairing with that order?";
                case "job_interview" -> "That is very interesting experience. Could you give me a specific example of how you resolved a tough problem under a tight deadline?";
                case "hotel_concierge" -> "Certainly, I would be delighted to assist you with that. Would you like a reservation made for tonight or tomorrow morning?";
                case "coffee_shop" -> "Coming right up! Would you like whole milk, skim milk, or a plant-based alternative such as oat or almond milk?";
                case "tech_presentation" -> "Fascinating perspective on the architecture. How do you plan to handle enterprise data governance and customer onboarding at scale?";
                default -> "Thank you for sharing that! Could you elaborate on what you mean and how you would approach the next step?";
            };
        };
    }

    private String adaptGreetingForLanguage(String greeting, String targetLanguage) {
        return adaptGreetingForScenarioAndLanguage("default", greeting, targetLanguage);
    }

    private String adaptGreetingForScenarioAndLanguage(String scenarioCode, String englishGreeting, String targetLanguage) {
        String lang = SupportedLanguages.normalizeOrDefault(targetLanguage, SupportedLanguages.DEFAULT);
        if ("Inglês".equals(lang)) {
            return englishGreeting;
        }
        return switch (lang) {
            case "Espanhol" -> switch (scenarioCode) {
                case "airport_checkin" -> "¡Hola! Bienvenido al mostrador de facturación. ¿Puedo ver su pasaporte y boleto, por favor?";
                case "restaurant_order" -> "¡Buenas noches! Bienvenido a nuestro restaurante. ¿Le gustaría pedir algo para beber mientras ve el menú?";
                case "job_interview" -> "¡Bienvenido! Gracias por acompañarnos hoy. Para empezar, ¿podría hablarme un poco sobre su experiencia profesional?";
                case "hotel_concierge" -> "¡Buenos días! Soy el conserje del hotel. ¿En qué puedo ayudarle durante su estancia hoy?";
                case "coffee_shop" -> "¡Hola! Bienvenido a nuestra cafetería. ¿Qué desea tomar hoy?";
                case "tech_presentation" -> "Gracias por su presentación. Mirando sus proyecciones, ¿cuál es su principal diferenciador competitivo?";
                default -> "¡Hola! Bienvenido. ¿En qué puedo ayudarte el día de hoy?";
            };
            case "Francês" -> switch (scenarioCode) {
                case "airport_checkin" -> "Bonjour ! Bienvenue au comptoir d'enregistrement. Puis-je voir votre passeport et votre billet, s'il vous plaît ?";
                case "restaurant_order" -> "Bonsoir et bienvenue dans notre bistrot ! Souhaitez-vous commencer par une boisson ou découvrir nos spécialités ?";
                case "job_interview" -> "Bonjour et merci d'être avec nous aujourd'hui. Pouvez-vous vous présenter brièvement et parler de votre parcours ?";
                case "hotel_concierge" -> "Bonjour ! Comment puis-je vous aider pour rendre votre séjour plus agréable ?";
                case "coffee_shop" -> "Salut et bienvenue au café ! Qu'est-ce qui vous ferait plaisir aujourd'hui ?";
                case "tech_presentation" -> "Merci pour cette présentation. Quelle est votre stratégie principale pour fidéliser vos clients ?";
                default -> "Bonjour et bienvenue ! Comment puis-je vous aider aujourd'hui ?";
            };
            case "Alemão" -> switch (scenarioCode) {
                case "airport_checkin" -> "Guten Tag! Willkommen am Check-in-Schalter. Darf ich bitte Ihren Reisepass und Ihr Ticket sehen?";
                case "restaurant_order" -> "Guten Abend! Willkommen in unserem Bistro. Möchten Sie zunächst ein Getränk bestellen?";
                case "job_interview" -> "Herzlich willkommen! Schön, dass Sie heute da sind. Könnten Sie sich kurz vorstellen?";
                case "hotel_concierge" -> "Guten Tag! Wie kann ich Ihnen heute während Ihres Aufenthalts weiterhelfen?";
                case "coffee_shop" -> "Hallo! Willkommen im Café. Was darf ich Ihnen heute zubereiten?";
                case "tech_presentation" -> "Vielen Dank für Ihren Pitch. Was ist das wichtigste Alleinstellungsmerkmal Ihres Produkts?";
                default -> "Hallo und willkommen! Wie kann ich Ihnen heute helfen?";
            };
            case "Italiano" -> switch (scenarioCode) {
                case "airport_checkin" -> "Buongiorno! Benvenuto al banco del check-in. Posso vedere il suo passaporto e il biglietto, per favore?";
                case "restaurant_order" -> "Buonasera! Benvenuto nel nostro ristorante. Desidera ordinare qualcosa da bere per iniziare?";
                case "job_interview" -> "Benvenuto! Grazie per essere qui oggi. Può parlarci un po' del suo percorso professionale?";
                case "hotel_concierge" -> "Buongiorno! Come posso assisterla durante il suo soggiorno oggi?";
                case "coffee_shop" -> "Ciao! Benvenuto al bar. Che cosa posso prepararti oggi?";
                case "tech_presentation" -> "Grazie per la presentazione. Qual è la sua strategia principale per crescere rapidamente sul mercato?";
                default -> "Ciao e benvenuto! Come posso aiutarti oggi?";
            };
            case "Japonês" -> switch (scenarioCode) {
                case "airport_checkin" -> "Konnichiwa! Chekku-in kauntā e yōkoso. Pasupōto to chiketto wo misete itadakemasu ka? (パスポートとチケットを見せていただけますか？)";
                case "restaurant_order" -> "Irasshaimase! Resutoran e yōkoso. Mazu onomimono wa ikaga desu ka? (まずお飲み物はいかがですか？)";
                case "job_interview" -> "Konnichiwa! Honjitsu wa arigatou gozaimasu. Mazu kantan ni jikoshōkai wo onegaishimasu. (まず簡単に自己紹介をお願いします。)";
                case "hotel_concierge" -> "Irasshaimase! Kyō wa dono yō na goyō deshō ka? Kankō annai mo dekimasu yo. (今日はどのようなご用でしょうか？)";
                case "coffee_shop" -> "Irasshaimase! Kafe e yōkoso. Kyō wa nani wo gochūmon nasaimasu ka? (今日は何をご注文なさいますか？)";
                case "tech_presentation" -> "Gohappyō arigatou gozaimasu! Kono sābisu no ichiban no tsuyomi wa nan desu ka? (一番の強みは何ですか？)";
                default -> "Konnichiwa! Yōkoso! Kyō wa nani wo renshū shimashō ka? (今日は何を練習しましょうか？)";
            };
            default -> englishGreeting;
        };
    }

    private List<String> localizedTargetPhrases(String scenarioCode, String targetLanguage, List<String> fallback) {
        String lang = SupportedLanguages.normalizeOrDefault(targetLanguage, SupportedLanguages.DEFAULT);
        return switch (lang) {
            case "Espanhol" -> switch (scenarioCode) {
                case "airport_checkin" -> List.of("Aquí tiene mi pasaporte", "¿Puedo tener asiento de ventanilla?", "Tengo una maleta para facturar");
                case "restaurant_order" -> List.of("¿Qué nos recomienda hoy?", "Me gustaría pedir la especialidad, por favor", "¿Nos trae la cuenta, por favor?");
                case "job_interview" -> List.of("En mi puesto anterior trabajé con...", "Mi mayor fortaleza es resolver problemas", "Estoy muy motivado por aprender");
                case "hotel_concierge" -> List.of("¿Podría recomendarme un buen museo?", "El aire acondicionado no funciona", "¿Puede llamar un taxi para las ocho?");
                case "coffee_shop" -> List.of("Quisiera un capuchino mediano, por favor", "Con leche de avena, por favor", "¿Cuánto cuesta el cruasán?");
                default -> List.of("Nuestra ventaja principal es la innovación", "Hemos validado este mercado recientemente");
            };
            case "Francês" -> switch (scenarioCode) {
                case "airport_checkin" -> List.of("Voici mon passeport", "Puis-je avoir une place côté fenêtre ?", "J'ai un bagage à enregistrer");
                case "restaurant_order" -> List.of("Que recommandez-vous aujourd'hui ?", "Je voudrais le plat du jour, s'il vous plaît", "L'addition, s'il vous plaît");
                case "job_interview" -> List.of("Dans mon poste précédent, j'ai géré...", "Mon point fort est l'organisation", "J'aime relever de nouveaux défis");
                case "hotel_concierge" -> List.of("Pouvez-vous me recommander un musée ?", "La climatisation ne marche pas", "Pourriez-vous appeler un taxi ?");
                case "coffee_shop" -> List.of("Je voudrais un café crème, s'il vous plaît", "Avec du lait d'avoine, merci", "Combien coûte le croissant ?");
                default -> List.of("Notre avantage principal est la qualité", "Nous visons une croissance durable");
            };
            case "Alemão" -> switch (scenarioCode) {
                case "airport_checkin" -> List.of("Hier ist mein Reisepass", "Kann ich einen Fensterplatz haben?", "Ich habe einen Koffer zum Aufgeben");
                case "restaurant_order" -> List.of("Was können Sie heute empfehlen?", "Ich hätte gern das Tagesgericht, bitte", "Die Rechnung, bitte");
                case "job_interview" -> List.of("In meiner letzten Position habe ich...", "Meine größte Stärke ist Teamarbeit", "Ich lerne sehr schnell neue Systeme");
                case "hotel_concierge" -> List.of("Können Sie ein gutes Museum empfehlen?", "Die Klimaanlage funktioniert nicht", "Könnten Sie mir bitte ein Taxi rufen?");
                case "coffee_shop" -> List.of("Ich möchte bitte einen Cappuccino", "Mit Hafermilch, bitte", "Was kostet das Croissant?");
                default -> List.of("Unser Hauptvorteil ist die Skalierbarkeit", "Wir fokussieren uns auf langfristige Kunden");
            };
            case "Italiano" -> switch (scenarioCode) {
                case "airport_checkin" -> List.of("Ecco il mio passaporto", "Posso avere un posto al finestrino?", "Ho una valigia da imbarcare");
                case "restaurant_order" -> List.of("Che cosa ci consiglia oggi?", "Vorrei ordinare un piatto tipico, per favore", "Ci porta il conto, per favore?");
                case "job_interview" -> List.of("Nel mio lavoro precedente ho gestito...", "Il mio punto di forza è la collaborazione", "Sono entusiasta di questa opportunità");
                case "hotel_concierge" -> List.of("Può consigliarmi un museo vicino?", "L'aria condizionata non funziona", "Potrebbe chiamarmi un taxi?");
                case "coffee_shop" -> List.of("Vorrei un cappuccino caldo, per favore", "Con latte d'avena, grazie", "Quanto costa il cornetto?");
                default -> List.of("Il nostro punto di forza è l'esperienza utente", "Abbiamo già validato il mercato locale");
            };
            case "Japonês" -> switch (scenarioCode) {
                case "airport_checkin" -> List.of("Kore ga watashi no pasupōto desu", "Madogawa no seki wo onegaishimasu", "Nimotsu wo hitotsu azuketai desu");
                case "restaurant_order" -> List.of("Osusume wa nan desu ka?", "Kore wo hitotsu kudasai", "Okaikei wo onegaishimasu");
                case "job_interview" -> List.of("Mae no shigoto de wa chīmu wo rīdo shimashita", "Watashi no tsuyomi wa mondai kaiketsu desu", "Atarashii koto wo manabu no ga suki desu");
                case "hotel_concierge" -> List.of("Chikaku no osusume no hakubutsukan wa doko desu ka?", "Eakon ga ugokimasen", "Takushī wo yonde itadakemasu ka?");
                case "coffee_shop" -> List.of("Kapuchīno wo hitotsu onegaishimasu", "Ōtsu miruku de onegaishimasu", "Kuroassan wa ikura desu ka?");
                default -> List.of("Watashitachi no tsuyomi wa gijutsuryoku desu", "Kokyaku manzokudo ga totemo takai desu");
            };
            default -> fallback;
        };
    }
}
