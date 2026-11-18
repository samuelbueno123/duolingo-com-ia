package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.duolinfo.ia.proj.util.SupportedLanguages;

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
        List<PronunciationChallenge> initialChallenges = List.of(
            // Inglês - A1..B2
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

            // Espanhol - A1..B2
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

            PronunciationChallenge.builder()
                .language("Espanhol")
                .cefrLevel("A2")
                .category("Cultura e Lazer")
                .phraseText("Ayer visitamos el museo de arte moderno con unos amigos.")
                .phoneticGuide("/aˈʝeɾ βisiˈtamos el muˈseo ðe ˈaɾte moˈðeɾno kon ˈunos aˈmiɣos/")
                .translation("Ontem visitamos o museu de arte moderna com alguns amigos.")
                .phoneticTips("Mantenha as cinco vogais do espanhol sempre curtas e bem abertas, sem nasalizar.")
                .difficultyScore(2)
                .build(),

            PronunciationChallenge.builder()
                .language("Espanhol")
                .cefrLevel("B1")
                .category("Expressões Naturais")
                .phraseText("Ojalá tengamos tiempo para visitar el centro histórico de la ciudad.")
                .phoneticGuide("/oxaˈla teŋˈɡamos ˈtjempo ˈpaɾa βisiˈtaɾ el ˈsentɾo isˈtoɾiko ðe la sjuˈðað/")
                .translation("Tomara que tenhamos tempo para visitar o centro histórico da cidade.")
                .phoneticTips("O som da letra 'j' em 'Ojalá' é emitido na garganta, como um 'r' forte em português.")
                .difficultyScore(3)
                .build(),

            PronunciationChallenge.builder()
                .language("Espanhol")
                .cefrLevel("B2")
                .category("Comunicação Profissional")
                .phraseText("Es fundamental que desarrollemos estrategias sostenibles a largo plazo.")
                .phoneticGuide("/es fundamenˈtal ke desaɾoˈʝemos estɾaˈtexjas sosteˈniβles a ˈlaɾɣo ˈplaso/")
                .translation("É fundamental que desenvolvamos estratégias sustentáveis a longo prazo.")
                .phoneticTips("Vibre o 'rr' múltiplo em 'desarrollemos' e marque o ritmo claro entre as sílabas.")
                .difficultyScore(4)
                .build(),

            // Francês - A1..B2
            PronunciationChallenge.builder()
                .language("Francês")
                .cefrLevel("A1")
                .category("Cumprimentos")
                .phraseText("Bonjour, je m'appelle Lucas et j'habite au Brésil.")
                .phoneticGuide("/bɔ̃ʒuʁ ʒə mapɛl lyka e ʒabit o bʁezil/")
                .translation("Bom dia, eu me chamo Lucas e moro no Brasil.")
                .phoneticTips("O 'u' em 'Lucas' se pronuncia fazendo biquinho com os lábios como se fosse falar 'i'; o 'r' é suave na garganta.")
                .difficultyScore(1)
                .build(),

            PronunciationChallenge.builder()
                .language("Francês")
                .cefrLevel("A2")
                .category("Café e Bistrô")
                .phraseText("Je voudrais un croissant chaud et un café au lait, s'il vous plaît.")
                .phoneticGuide("/ʒə vudʁɛ œ̃ kʁwasɑ̃ ʃo e œ̃ kafe o lɛ sil vu plɛ/")
                .translation("Eu gostaria de um croissant quentinho e um café com leite, por favor.")
                .phoneticTips("Em 'voudrais' e 'lait', o conjunto 'ai' tem som de 'é' aberto; não pronuncie o 't' ou 's' final.")
                .difficultyScore(2)
                .build(),

            PronunciationChallenge.builder()
                .language("Francês")
                .cefrLevel("B1")
                .category("Viagens e Transportes")
                .phraseText("Nous allons prendre le train à grande vitesse demain matin.")
                .phoneticGuide("/nu zalɔ̃ pʁɑ̃dʁ lə tʁɛ̃ a ɡʁɑ̃d vitɛs dəmɛ̃ matɛ̃/")
                .translation("Nós vamos pegar o trem de alta velocidade amanhã de manhã.")
                .phoneticTips("Faça a liaison entre 'nous' e 'allons' (/nuzalɔ̃/) unindo o 's' com som de 'z' à vogal seguinte.")
                .difficultyScore(3)
                .build(),

            PronunciationChallenge.builder()
                .language("Francês")
                .cefrLevel("B2")
                .category("Expressão Profissional")
                .phraseText("Bien que ce projet soit ambitieux, notre équipe relèvera ce défi ensemble.")
                .phoneticGuide("/bjɛ̃ kə sə pʁɔʒɛ swa ɑ̃bisjø nɔtʁ ekijp ʁəlɛvʁa sə defi ɑ̃sɑ̃bl/")
                .translation("Embora este projeto seja ambicioso, nossa equipe vencerá esse desafio unida.")
                .phoneticTips("Capriche nas vogais nasais em 'bien', 'ambitieux' e 'ensemble' sem encostar a língua no palato.")
                .difficultyScore(4)
                .build(),

            // Alemão - A1..B2
            PronunciationChallenge.builder()
                .language("Alemão")
                .cefrLevel("A1")
                .category("Apresentação Pessoal")
                .phraseText("Guten Tag, ich komme aus Brasilien und lerne Deutsch.")
                .phoneticGuide("/ˈɡuːtn̩ taːk ɪç ˈkɔmə aʊ̯s bʁaˈziːli̯ən ʊnt ˈlɛʁnə dɔɪ̯tʃ/")
                .translation("Boa tarde, eu venho do Brasil e estou aprendendo alemão.")
                .phoneticTips("Em 'ich', o 'ch' tem som suave de ar passando pelo palato; em 'Deutsch', 'eu' soa como 'ói'.")
                .difficultyScore(1)
                .build(),

            PronunciationChallenge.builder()
                .language("Alemão")
                .cefrLevel("A2")
                .category("Cidade e Transportes")
                .phraseText("Entschuldigung, wie komme ich bitte zum Hauptbahnhof?")
                .phoneticGuide("/ɛntˈʃʊldɪɡʊŋ viː ˈkɔmə ɪç ˈbɪtə t͡sʊm ˈhaʊ̯ptbaːnhoːf/")
                .translation("Com licença, como chego à estação central, por favor?")
                .phoneticTips("Pronuncie 'sch' como 'ch' em 'chave' e lembre que o 'z' em 'zum' tem som firme de 'ts'.")
                .difficultyScore(2)
                .build(),

            PronunciationChallenge.builder()
                .language("Alemão")
                .cefrLevel("B1")
                .category("Convivência e Planos")
                .phraseText("Ich freue mich darauf, nächste Woche neue Kollegen kennenzulernen.")
                .phoneticGuide("/ɪç ˈfʁɔɪ̯ə mɪç daˈʁaʊ̯f ˈnɛːçstə ˈvɔxə ˈnɔɪ̯ə kɔˈleːɡn̩ ˈkɛnənt͡suˌlɛʁnən/")
                .translation("Estou feliz por conhecer novos colegas na próxima semana.")
                .phoneticTips("Diferencie 'ch' suave após 'ä/e' em 'nächste' do 'ch' gutural após 'o' em 'Woche'.")
                .difficultyScore(3)
                .build(),

            PronunciationChallenge.builder()
                .language("Alemão")
                .cefrLevel("B2")
                .category("Ambiente Corporativo")
                .phraseText("Obwohl die Aufgabe anspruchsvoll ist, finden wir gemeinsam eine gute Lösung.")
                .phoneticGuide("/ɔpˈvoːl diː ˈaʊ̯fɡaːbə ˈanʃpʁʊxsfɔl ɪst ˈfɪndn̩ viːɐ̯ ɡəˈmaɪ̯nzaːm ˈaɪ̯nə ˈɡuːtə ˈløːzʊŋ/")
                .translation("Embora a tarefa seja exigente, encontraremos juntos uma boa solução.")
                .phoneticTips("Para o 'ö' em 'Lösung', arredonde os lábios para dizer 'ô' enquanto articula a vogal 'ê'.")
                .difficultyScore(4)
                .build(),

            // Italiano - A1..B2
            PronunciationChallenge.builder()
                .language("Italiano")
                .cefrLevel("A1")
                .category("Cumprimentos")
                .phraseText("Buongiorno! Piacere di conoscerti, come stai oggi?")
                .phoneticGuide("/bwɔnˈdʒorno pjaˈtʃeːre di koˈnoʃʃerti ˈkoːme ˈstai ˈɔddʒi/")
                .translation("Bom dia! Prazer em conhecê-lo, como você está hoje?")
                .phoneticTips("Articule claramente 'gi' e 'ce' com som de 'dj' e 'tch'; em 'oggi', prolongue levemente o duplo 'gg'.")
                .difficultyScore(1)
                .build(),

            PronunciationChallenge.builder()
                .language("Italiano")
                .cefrLevel("A2")
                .category("Gastronomia")
                .phraseText("Vorrei ordinare un caffè espresso e una pizza margherita, per favore.")
                .phoneticGuide("/vorˈrɛi ordiˈnaːre un kafˈfɛ esˈprɛsso e ˈuːna ˈpittsa marɡeˈriːta per faˈvoːre/")
                .translation("Eu gostaria de pedir um café expresso e uma pizza margherita, por favor.")
                .phoneticTips("Marque as consoantes duplas em 'vorrei', 'caffè' e 'pizza' ('pít-tsa'); em 'margherita', 'gh' tem som duro de 'g'.")
                .difficultyScore(2)
                .build(),

            PronunciationChallenge.builder()
                .language("Italiano")
                .cefrLevel("B1")
                .category("Viagens pela Itália")
                .phraseText("Domani mattina prenderemo il treno veloce da Roma a Firenze.")
                .phoneticGuide("/doˈmaːni matˈtiːna prendeˈreːmo il ˈtreːno veˈloːtʃe da ˈroːma a fiˈrɛntse/")
                .translation("Amanhã de manhã pegaremos o trem rápido de Roma para Florença.")
                .phoneticTips("Em 'Firenze', o 'z' é pronunciado como 'ts', enquanto o 'c' em 'veloce' soa como 'tch'.")
                .difficultyScore(3)
                .build(),

            PronunciationChallenge.builder()
                .language("Italiano")
                .cefrLevel("B2")
                .category("Opiniões e Cultura")
                .phraseText("Penso che questa esperienza sia davvero straordinaria per la nostra crescita.")
                .phoneticGuide("/ˈpɛnso ke ˈkwesta espeˈrjɛntsa ˈsiːa davˈveːro straordiˈnaːrja per la ˈnɔstra ˈkreʃʃita/")
                .translation("Penso que esta experiência seja realmente extraordinária para o nosso crescimento.")
                .phoneticTips("Em 'che', o dígrafo 'ch' sempre soa como 'k'; em 'crescita', 'sc' antes de 'i' tem som de 'ch'.")
                .difficultyScore(4)
                .build(),

            // Japonês - A1..B2
            PronunciationChallenge.builder()
                .language("Japonês")
                .cefrLevel("A1")
                .category("Cumprimentos")
                .phraseText("おはようございます はじめまして")
                .phoneticGuide("/o-ha-yō go-za-i-ma-su | ha-ji-me-ma-shi-te/")
                .translation("Bom dia! Muito prazer em conhecê-lo(a).")
                .phoneticTips("Mantenha cada sílaba (mora) com ritmo uniforme e reduza discretamente o som de 'u' no final de 'gozaimasu'.")
                .difficultyScore(1)
                .build(),

            PronunciationChallenge.builder()
                .language("Japonês")
                .cefrLevel("A2")
                .category("Café e Pedidos")
                .phraseText("すみません コーヒーを ください")
                .phoneticGuide("/su-mi-ma-se-n | kō-hī o ku-da-sa-i/")
                .translation("Com licença, um café, por favor.")
                .phoneticTips("Alongue bem as duas vogais em 'kōhī' (ko-o-hi-i) e pronuncie 'を' suavemente como 'o'.")
                .difficultyScore(2)
                .build(),

            PronunciationChallenge.builder()
                .language("Japonês")
                .cefrLevel("B1")
                .category("Transporte e Cidade")
                .phraseText("すみません 駅は どこですか")
                .phoneticGuide("/su-mi-ma-se-n | e-ki wa do-ko de-su ka/")
                .translation("Com licença, onde fica a estação de trem?")
                .phoneticTips("Como partícula de tópico, 'は' lê-se 'wa' e não 'ha'; eleve levemente a entonação no 'ka' final de pergunta.")
                .difficultyScore(3)
                .build(),

            PronunciationChallenge.builder()
                .language("Japonês")
                .cefrLevel("B2")
                .category("Cortesia Profissional")
                .phraseText("いつも お世話に なっております")
                .phoneticGuide("/i-tsu-mo o-se-wa ni na-t-te o-ri-ma-su/")
                .translation("Agradeço sempre pelo seu apoio e colaboração.")
                .phoneticTips("Em 'natte', faça uma pequena retenção antes do 't' (sokuon 'っ') para marcar a consoante dupla.")
                .difficultyScore(4)
                .build()
        );

        Map<String, Set<String>> existingByLanguage = new HashMap<>();
        List<PronunciationChallenge> toInsert = new ArrayList<>();
        for (PronunciationChallenge candidate : initialChallenges) {
            String lang = candidate.getLanguage();
            Set<String> existingPhrases = existingByLanguage.computeIfAbsent(lang, key ->
                challengeRepository.findByLanguageIgnoreCase(key).stream()
                    .map(c -> c.getPhraseText() != null ? c.getPhraseText().trim().toLowerCase(Locale.ROOT) : "")
                    .collect(Collectors.toSet())
            );
            String normalizedPhrase = candidate.getPhraseText().trim().toLowerCase(Locale.ROOT);
            if (existingPhrases.add(normalizedPhrase)) {
                toInsert.add(candidate);
            }
        }

        if (!toInsert.isEmpty()) {
            challengeRepository.saveAll(toInsert);
            log.info("Inicializados {} desafios de pronúncia nos 6 idiomas suportados.", toInsert.size());
        }
    }

    public List<PronunciationChallengeDTO> getChallenges(String language, String cefrLevel) {
        String lang = SupportedLanguages.normalizeOrDefault(language, SupportedLanguages.DEFAULT);
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
        String clean = phrase.replaceAll("[^\\p{L}\\p{N}\\s]", " ").trim().toLowerCase(Locale.ROOT);
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
            if (xpAmount <= 0 || language == null || language.isBlank()) {
                return;
            }
            studentRepository.findById(studentId).ifPresent(student -> {
                StudentLanguage sl = studentLanguageRepository
                        .findByStudentAndLanguageNameIgnoreCase(student, language.trim())
                        .orElse(null);
                if (sl != null) {
                    sl.setScore((sl.getScore() != null ? sl.getScore() : 0) + xpAmount);
                    studentLanguageRepository.save(sl);
                } else {
                    StudentLanguage newLang = new StudentLanguage();
                    newLang.setStudent(student);
                    newLang.setLanguageName(language.trim());
                    newLang.setLevel("Básico");
                    newLang.setScore(xpAmount);
                    studentLanguageRepository.save(newLang);
                }
            });
        } catch (Exception e) {
            log.warn("Erro ao atribuir XP de pronúncia: {}", e.getMessage());
        }
    }
}
