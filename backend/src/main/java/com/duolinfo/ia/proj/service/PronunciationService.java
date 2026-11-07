package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.pronunciation.PronunciationChallengeDTO;
import com.duolinfo.ia.proj.dto.pronunciation.PronunciationEvaluationRequestDTO;
import com.duolinfo.ia.proj.dto.pronunciation.PronunciationEvaluationResponseDTO;
import com.duolinfo.ia.proj.dto.pronunciation.PronunciationStatsDTO;
import com.duolinfo.ia.proj.dto.pronunciation.WordPronunciationFeedbackDTO;
import com.duolinfo.ia.proj.entity.PronunciationAttempt;
import com.duolinfo.ia.proj.entity.PronunciationChallenge;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.repository.PronunciationAttemptRepository;
import com.duolinfo.ia.proj.repository.PronunciationChallengeRepository;
import com.duolinfo.ia.proj.repository.StudentLanguageRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class PronunciationService {

    private static final Logger log = LoggerFactory.getLogger(PronunciationService.class);

    private final PronunciationChallengeRepository challengeRepository;
    private final PronunciationAttemptRepository attemptRepository;
    private final StudentRepository studentRepository;
    private final StudentLanguageRepository studentLanguageRepository;
    private final ObjectMapper objectMapper;

    public PronunciationService(
            PronunciationChallengeRepository challengeRepository,
            PronunciationAttemptRepository attemptRepository,
            StudentRepository studentRepository,
            StudentLanguageRepository studentLanguageRepository,
            ObjectMapper objectMapper) {
        this.challengeRepository = challengeRepository;
        this.attemptRepository = attemptRepository;
        this.studentRepository = studentRepository;
        this.studentLanguageRepository = studentLanguageRepository;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedInitialChallenges() {
        if (challengeRepository.count() > 0) {
            return;
        }

        List<PronunciationChallenge> initialChallenges = List.of(
            // Inglês - A1
            PronunciationChallenge.builder()
                .language("Inglês")
                .cefrLevel("A1")
                .category("Cumprimentos")
                .phraseText("Good morning, nice to meet you!")
                .phoneticGuide("/ɡʊd ˈmɔːrnɪŋ naɪs tuː miːt juː/")
                .translation("Bom dia, prazer em conhecer você!")
                .phoneticTips("Ligue as palavras 'nice' e 'to' com suavidade, sem pausar bruscamente.")
                .difficultyScore(1)
                .build(),

            PronunciationChallenge.builder()
                .language("Inglês")
                .cefrLevel("A1")
                .category("Restaurante")
                .phraseText("I would like a cup of coffee with milk, please.")
                .phoneticGuide("/aɪ wʊd laɪk ə kʌp əv ˈkɔːfi wɪð mɪlk pliːz/")
                .translation("Eu gostaria de uma xícara de café com leite, por favor.")
                .phoneticTips("No 'would like', o 'l' em 'would' é mudo.")
                .difficultyScore(1)
                .build(),

            // Inglês - A2
            PronunciationChallenge.builder()
                .language("Inglês")
                .cefrLevel("A2")
                .category("Fonema /θ/ e /ð/")
                .phraseText("I think that this theater is fantastic.")
                .phoneticGuide("/aɪ θɪŋk ðæt ðɪs ˈθiːətər ɪz fænˈtæstɪk/")
                .translation("Eu acho que este teatro é fantástico.")
                .phoneticTips("Coloque suavemente a ponta da língua entre os dentes ao pronunciar 'think', 'that' e 'theater'.")
                .difficultyScore(2)
                .build(),

            // Inglês - B1
            PronunciationChallenge.builder()
                .language("Inglês")
                .cefrLevel("B1")
                .category("Viagem e Aeroporto")
                .phraseText("Could you please tell me where the baggage claim area is?")
                .phoneticGuide("/kʊd juː pliːz tɛl miː wɛər ðə ˈbæɡɪdʒ kleɪm ˈɛəriə ɪz/")
                .translation("Você poderia por favor me dizer onde fica a área de retirada de bagagens?")
                .phoneticTips("Entonação ascendente no início e descendente suave no final de perguntas formais.")
                .difficultyScore(3)
                .build(),

            // Inglês - B2
            PronunciationChallenge.builder()
                .language("Inglês")
                .cefrLevel("B2")
                .category("Apresentação Profissional")
                .phraseText("Our multidisciplinary team developed an innovative solution to increase overall productivity.")
                .phoneticGuide("/ˈaʊər ˌmʌl.tiˈdɪs.ə.plɪ.ner.i tiːm dɪˈvɛl.əpt ən ˈɪn.ə.veɪ.tɪv səˈluː.ʃən tuː ɪnˈkriːs ˌoʊ.vɚˈɔːl ˌproʊ.dʌkˈtɪv.ə.t̬i/")
                .translation("Nossa equipe multidisciplinar desenvolveu uma solução inovadora para aumentar a produtividade geral.")
                .phoneticTips("Atenção à tonicidade em 'innovative' e à redução vocálica schwa /ə/ em sílabas átonas.")
                .difficultyScore(4)
                .build(),

            // Espanhol - A1
            PronunciationChallenge.builder()
                .language("Espanhol")
                .cefrLevel("A1")
                .category("Cotidiano")
                .phraseText("Muchas gracias por su amable ayuda.")
                .phoneticGuide("/ˈmutʃas ˈɡɾasjas poɾ su aˈmaβle aˈʝuða/")
                .translation("Muito obrigado por sua gentil ajuda.")
                .phoneticTips("O 'r' brando em 'gracias' e 'por' é produzido com um toque rápido da língua no céu da boca.")
                .difficultyScore(1)
                .build(),

            // Espanhol - B1
            PronunciationChallenge.builder()
                .language("Espanhol")
                .cefrLevel("B1")
                .category("Expressões Naturais")
                .phraseText("Ojalá tengamos tiempo para visitar el centro histórico de la ciudad.")
                .phoneticGuide("/oxaˈla teŋˈɡamos ˈtjempo ˈpaɾa βisiˈtaɾ el ˈsentɾo isˈtoɾiko ðe la sjuˈðað/")
                .translation("Tomara que tenhamos tempo para visitar o centro histórico da cidade.")
                .phoneticTips("O som da letra 'j' em 'Ojalá' é emitido na garganta, como um 'r' forte em português.")
                .difficultyScore(3)
                .build()
        );

        challengeRepository.saveAll(initialChallenges);
        log.info("Inicializados {} desafios de pronúncia padrão no banco de dados.", initialChallenges.size());
    }

    public List<PronunciationChallengeDTO> getChallenges(String language, String cefrLevel) {
        String lang = (language != null && !language.isBlank()) ? language.trim() : "Inglês";
        List<PronunciationChallenge> challenges;

        if (cefrLevel != null && !cefrLevel.isBlank()) {
            challenges = challengeRepository.findByLanguageIgnoreCaseAndCefrLevelIgnoreCase(lang, cefrLevel.trim());
        } else {
            challenges = challengeRepository.findByLanguageIgnoreCase(lang);
        }

        return challenges.stream()
                .map(PronunciationChallengeDTO::fromEntity)
                .toList();
    }

    public PronunciationChallengeDTO getChallengeById(Long id) {
        PronunciationChallenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Desafio de pronúncia não encontrado."));
        return PronunciationChallengeDTO.fromEntity(challenge);
    }

    @Transactional
    public PronunciationEvaluationResponseDTO evaluateAttempt(Long studentId, PronunciationEvaluationRequestDTO request) {
        PronunciationChallenge challenge = challengeRepository.findById(request.challengeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Desafio não encontrado."));

        String spoken = request.spokenText().trim();
        List<WordPronunciationFeedbackDTO> wordBreakdown = analyzeSpeech(challenge.getPhraseText(), spoken);

        // Calculate scores
        long totalWords = wordBreakdown.stream().filter(w -> !"EXTRA".equals(w.status())).count();
        long matchedWords = wordBreakdown.stream().filter(w -> "PERFECT".equals(w.status()) || "ACCEPTABLE".equals(w.status())).count();
        long perfectWords = wordBreakdown.stream().filter(w -> "PERFECT".equals(w.status())).count();

        double completenessScore = totalWords > 0 ? (double) matchedWords / totalWords * 100.0 : 0.0;
        double accuracyScore = totalWords > 0
                ? wordBreakdown.stream().filter(w -> !"EXTRA".equals(w.status())).mapToDouble(WordPronunciationFeedbackDTO::similarityScore).average().orElse(0.0) * 100.0
                : 0.0;

        double finalScore = Math.round(((accuracyScore * 0.6) + (completenessScore * 0.4)) * 10.0) / 10.0;

        String summary = generateFeedbackSummary(finalScore, perfectWords, totalWords, challenge.getPhoneticTips());

        int xp = 0;
        if (finalScore >= 80.0) {
            xp = 15;
        } else if (finalScore >= 50.0) {
            xp = 8;
        } else {
            xp = 3;
        }

        String breakdownJson = "";
        try {
            breakdownJson = objectMapper.writeValueAsString(wordBreakdown);
        } catch (Exception e) {
            breakdownJson = "[]";
        }

        PronunciationAttempt attempt = PronunciationAttempt.builder()
                .studentId(studentId)
                .challengeId(challenge.getId())
                .spokenText(spoken)
                .accuracyScore(accuracyScore)
                .completenessScore(completenessScore)
                .finalScore(finalScore)
                .wordBreakdownJson(breakdownJson)
                .feedbackSummary(summary)
                .attemptedAt(LocalDateTime.now())
                .build();

        attempt = attemptRepository.save(attempt);

        // Award XP
        awardExperience(studentId, challenge.getLanguage(), xp);

        return new PronunciationEvaluationResponseDTO(
            attempt.getId(),
            challenge.getId(),
            challenge.getPhraseText(),
            spoken,
            accuracyScore,
            completenessScore,
            finalScore,
            xp,
            summary,
            wordBreakdown,
            attempt.getAttemptedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<PronunciationEvaluationResponseDTO> getStudentHistory(Long studentId, Long challengeId) {
        List<PronunciationAttempt> attempts = challengeId != null
                ? attemptRepository.findByStudentIdAndChallengeIdOrderByAttemptedAtDesc(studentId, challengeId)
                : attemptRepository.findByStudentIdOrderByAttemptedAtDesc(studentId);

        return attempts.stream()
                .map(a -> {
                    List<WordPronunciationFeedbackDTO> breakdown = List.of();
                    try {
                        if (a.getWordBreakdownJson() != null && !a.getWordBreakdownJson().isBlank()) {
                            breakdown = objectMapper.readValue(a.getWordBreakdownJson(), new TypeReference<List<WordPronunciationFeedbackDTO>>() {});
                        }
                    } catch (Exception ignored) {}

                    String expectedPhrase = challengeRepository.findById(a.getChallengeId())
                            .map(PronunciationChallenge::getPhraseText)
                            .orElse("");

                    return new PronunciationEvaluationResponseDTO(
                        a.getId(),
                        a.getChallengeId(),
                        expectedPhrase,
                        a.getSpokenText(),
                        a.getAccuracyScore(),
                        a.getCompletenessScore(),
                        a.getFinalScore(),
                        0,
                        a.getFeedbackSummary(),
                        breakdown,
                        a.getAttemptedAt()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PronunciationStatsDTO getStudentStats(Long studentId) {
        List<PronunciationAttempt> attempts = attemptRepository.findByStudentIdOrderByAttemptedAtDesc(studentId);
        if (attempts.isEmpty()) {
            return new PronunciationStatsDTO(0, 0.0, 0, "N/A", "N/A");
        }

        double avg = attempts.stream().mapToDouble(PronunciationAttempt::getFinalScore).average().orElse(0.0);
        long mastered = attempts.stream().filter(a -> a.getFinalScore() >= 85.0).count();

        return new PronunciationStatsDTO(
            attempts.size(),
            Math.round(avg * 10.0) / 10.0,
            mastered,
            "Cumprimentos",
            "Fonemas Complexos"
        );
    }

    private List<WordPronunciationFeedbackDTO> analyzeSpeech(String expectedPhrase, String spokenText) {
        String[] expectedWords = cleanWords(expectedPhrase);
        String[] spokenWords = cleanWords(spokenText);

        List<WordPronunciationFeedbackDTO> feedback = new ArrayList<>();
        boolean[] spokenUsed = new boolean[spokenWords.length];

        for (String expWord : expectedWords) {
            double bestSim = 0.0;
            int bestIdx = -1;

            for (int j = 0; j < spokenWords.length; j++) {
                if (!spokenUsed[j]) {
                    double sim = calculateSimilarity(expWord, spokenWords[j]);
                    if (sim > bestSim) {
                        bestSim = sim;
                        bestIdx = j;
                    }
                }
            }

            if (bestIdx >= 0 && bestSim >= 0.70) {
                spokenUsed[bestIdx] = true;
                String status = bestSim >= 0.95 ? "PERFECT" : "ACCEPTABLE";
                feedback.add(new WordPronunciationFeedbackDTO(
                    expWord,
                    spokenWords[bestIdx],
                    status,
                    bestSim,
                    status.equals("PERFECT") ? "Pronúncia excelente!" : "Boa aproximação fonética."
                ));
            } else if (bestIdx >= 0 && bestSim >= 0.40) {
                spokenUsed[bestIdx] = true;
                feedback.add(new WordPronunciationFeedbackDTO(
                    expWord,
                    spokenWords[bestIdx],
                    "MISPRONOUNCED",
                    bestSim,
                    "Atenção à articulação e tonicidade desta palavra."
                ));
            } else {
                feedback.add(new WordPronunciationFeedbackDTO(
                    expWord,
                    "-",
                    "OMITTED",
                    0.0,
                    "Esta palavra não foi captada pelo microfone."
                ));
            }
        }

        // Catch extra spoken words
        for (int j = 0; j < spokenWords.length; j++) {
            if (!spokenUsed[j]) {
                feedback.add(new WordPronunciationFeedbackDTO(
                    "-",
                    spokenWords[j],
                    "EXTRA",
                    0.0,
                    "Palavra adicional não esperada nesta frase."
                ));
            }
        }

        return feedback;
    }

    private String[] cleanWords(String phrase) {
        if (phrase == null) return new String[0];
        String clean = phrase.replaceAll("[^a-zA-ZáàâãéèêíïóôõöúçñÁÀÂÃÉÈÊÍÏÓÔÕÖÚÇÑ\\s]", "").toLowerCase(Locale.ROOT);
        return Arrays.stream(clean.split("\\s+"))
                .filter(w -> !w.isBlank())
                .toArray(String[]::new);
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1.equalsIgnoreCase(s2)) return 1.0;
        int distance = computeLevenshtein(s1.toLowerCase(Locale.ROOT), s2.toLowerCase(Locale.ROOT));
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        return Math.max(0.0, 1.0 - ((double) distance / maxLen));
    }

    private int computeLevenshtein(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) costs[j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(
                    1 + Math.min(costs[j], costs[j - 1]),
                    s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1
                );
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[s2.length()];
    }

    private String generateFeedbackSummary(double finalScore, long perfectWords, long totalWords, String phoneticTips) {
        if (finalScore >= 90.0) {
            return "Pronúncia fantástica! Clareza, ritmo e dicção perfeitos (" + perfectWords + "/" + totalWords + " palavras com excelência).";
        } else if (finalScore >= 75.0) {
            return "Muito bom! Sua fala foi claramente compreensível. Dica: " + (phoneticTips != null ? phoneticTips : "continue praticando o ritmo da frase.");
        } else if (finalScore >= 50.0) {
            return "Boa tentativa! Algumas palavras precisam de maior precisão fonética. Dica: " + (phoneticTips != null ? phoneticTips : "ouça o áudio e repita pausadamente.");
        } else {
            return "Não se preocupe, pronúncia é questão de repetição constante! Experimente falar mais próximo ao microfone e repetir devagar.";
        }
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
            log.warn("Erro ao atribuir XP de pronúncia: {}", e.getMessage());
        }
    }
}
