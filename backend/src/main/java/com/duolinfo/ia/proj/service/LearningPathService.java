package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.learningpath.DiagnosticAnswerDTO;
import com.duolinfo.ia.proj.dto.learningpath.DiagnosticQuestionDTO;
import com.duolinfo.ia.proj.dto.learningpath.DiagnosticResultDTO;
import com.duolinfo.ia.proj.dto.learningpath.LearningGoalUpdateDTO;
import com.duolinfo.ia.proj.dto.learningpath.LearningPathDTO;
import com.duolinfo.ia.proj.dto.learningpath.ModuleProgressDTO;
import com.duolinfo.ia.proj.entity.LearningPathModule;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.entity.StudentLearningPath;
import com.duolinfo.ia.proj.entity.StudentModuleProgress;
import com.duolinfo.ia.proj.repository.LearningPathModuleRepository;
import com.duolinfo.ia.proj.repository.StudentLanguageRepository;
import com.duolinfo.ia.proj.repository.StudentLearningPathRepository;
import com.duolinfo.ia.proj.repository.StudentModuleProgressRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.util.SupportedLanguages;

@Service
public class LearningPathService {

    private static final Logger log = LoggerFactory.getLogger(LearningPathService.class);

    private final StudentLearningPathRepository pathRepository;
    private final LearningPathModuleRepository moduleRepository;
    private final StudentModuleProgressRepository progressRepository;
    private final StudentRepository studentRepository;
    private final StudentLanguageRepository studentLanguageRepository;

    private static final Map<String, List<DiagnosticQuestionDTO>> DIAGNOSTIC_QUESTIONS_BY_LANGUAGE = Map.of(
        "Inglês", List.of(
            new DiagnosticQuestionDTO(1, "A1", "Gramática", "Complete a frase: 'She ___ to the gym every morning.'", List.of("go", "goes", "is go", "going"), 1),
            new DiagnosticQuestionDTO(2, "A1", "Vocabulário", "Qual é o oposto da palavra 'expensive'?", List.of("cheap", "fast", "heavy", "difficult"), 0),
            new DiagnosticQuestionDTO(3, "A2", "Gramática", "Complete com o passado correto: 'Yesterday we ___ an interesting movie.'", List.of("watch", "watched", "did watched", "have watch"), 1),
            new DiagnosticQuestionDTO(4, "A2", "Vocabulário", "O que significa 'airport gate'?", List.of("Pista de pouso", "Portão de embarque", "Esteira de bagagens", "Guichê da alfândega"), 1),
            new DiagnosticQuestionDTO(5, "B1", "Gramática", "Escolha a opção correta: 'If it rains tomorrow, we ___ stay at home.'", List.of("would", "will", "are", "have"), 1),
            new DiagnosticQuestionDTO(6, "B1", "Vocabulário", "Complete com a preposição correta: 'I am really looking forward ___ meeting your family.'", List.of("for", "to", "at", "with"), 1),
            new DiagnosticQuestionDTO(7, "B2", "Gramática", "Identifique a sentença correta:", List.of(
                "Had I known about the schedule change, I would have notified you.",
                "If I would know about the schedule change, I told you.",
                "Had I knew about the schedule change, I had told you.",
                "If I had been known about the change, I will notify you."
            ), 0),
            new DiagnosticQuestionDTO(8, "B2", "Vocabulário", "O termo 'cutting-edge technology' é melhor traduzido como:", List.of("Tecnologia ultrapassada", "Tecnologia de ponta / inovadora", "Tecnologia de baixo custo", "Tecnologia arriscada"), 1)
        ),
        "Espanhol", List.of(
            new DiagnosticQuestionDTO(1, "A1", "Gramática", "Complete: 'Yo ___ estudiante de idiomas.'", List.of("es", "soy", "eres", "somos"), 1),
            new DiagnosticQuestionDTO(2, "A1", "Vocabulário", "Qual palavra significa 'café da manhã' em espanhol?", List.of("Almuerzo", "Cena", "Desayuno", "Merienda"), 2),
            new DiagnosticQuestionDTO(3, "A2", "Gramática", "Complete no pretérito: 'Ayer nosotros ___ al museo.'", List.of("fuimos", "vamos", "iremos", "iba"), 0),
            new DiagnosticQuestionDTO(4, "A2", "Vocabulário", "O que significa 'tarjeta de embarque'?", List.of("Cartão de crédito", "Cartão de embarque", "Mala de mão", "Passaporte"), 1),
            new DiagnosticQuestionDTO(5, "B1", "Gramática", "Escolha a forma correta do subjuntivo: 'Espero que ___ buen viaje.'", List.of("tienes", "tendrás", "tengas", "tuviste"), 2),
            new DiagnosticQuestionDTO(6, "B1", "Vocabulário", "Qual é a diferença entre 'por' e 'para' em 'Este regalo es ___ ti'?", List.of("por", "para", "de", "desde"), 1),
            new DiagnosticQuestionDTO(7, "B2", "Gramática", "Assinale a frase correta:", List.of(
                "Si hubiera tenido tiempo, habría visitado la exposición.",
                "Si tendría tiempo, visito la exposición.",
                "Si habría tenido tiempo, visité la exposición.",
                "Si tengo tiempo, habría visitado la exposición."
            ), 0),
            new DiagnosticQuestionDTO(8, "B2", "Vocabulário", "O termo 'sin embargo' equivale em português a:", List.of("Portanto", "No entanto / Contudo", "Em primeiro lugar", "Por isso"), 1)
        ),
        "Francês", List.of(
            new DiagnosticQuestionDTO(1, "A1", "Gramática", "Complete: 'Je ___ étudiant à Paris.'", List.of("es", "suis", "est", "sommes"), 1),
            new DiagnosticQuestionDTO(2, "A1", "Vocabulário", "Qual saudação significa 'Boa tarde / Boa noite ao chegar' em francês?", List.of("Merci", "Bonsoir", "Pardon", "Au revoir"), 1),
            new DiagnosticQuestionDTO(3, "A2", "Gramática", "Complete no passé composé: 'Hier, elle ___ un livre.'", List.of("a lu", "lit", "lira", "lisait"), 0),
            new DiagnosticQuestionDTO(4, "A2", "Vocabulário", "O que significa 'l'addition, s'il vous plaît'?", List.of("O cardápio", "A sobremesa", "A conta, por favor", "O prato do dia"), 2),
            new DiagnosticQuestionDTO(5, "B1", "Gramática", "Escolha o pronome correto: 'Tu vas au cinéma ? Oui, j'___ vais.'", List.of("en", "y", "le", "lui"), 1),
            new DiagnosticQuestionDTO(6, "B1", "Vocabulário", "O que significa 'être à l'heure'?", List.of("Estar atrasado", "Chegar no horário", "Perder o trem", "Tirar férias"), 1),
            new DiagnosticQuestionDTO(7, "B2", "Gramática", "Identifique a frase correta com subjuntivo:", List.of(
                "Il faut que nous fassions attention aux détails.",
                "Il faut que nous faisons attention aux détails.",
                "Il faut que nous ferons attention aux détails.",
                "Il faut nous faire attention aux détails."
            ), 0),
            new DiagnosticQuestionDTO(8, "B2", "Vocabulário", "O conectivo 'néanmoins' significa:", List.of("Entretanto / Apesar disso", "Simultaneamente", "Frequentemente", "Rapidamente"), 0)
        ),
        "Alemão", List.of(
            new DiagnosticQuestionDTO(1, "A1", "Gramática", "Complete: 'Ich ___ aus Brasilien.'", List.of("kommst", "komme", "kommt", "kommen"), 1),
            new DiagnosticQuestionDTO(2, "A1", "Vocabulário", "Qual palavra alemã significa 'Obrigado'?", List.of("Bitte", "Entschuldigung", "Danke", "Tschüss"), 2),
            new DiagnosticQuestionDTO(3, "A2", "Gramática", "Escolha o artigo acusativo correto: 'Ich kaufe ___ Tisch (der Tisch).'", List.of("der", "den", "dem", "des"), 1),
            new DiagnosticQuestionDTO(4, "A2", "Vocabulário", "O que significa 'der Bahnhof'?", List.of("Aeroporto", "Estação de trem", "Hotel", "Hospital"), 1),
            new DiagnosticQuestionDTO(5, "B1", "Gramática", "Onde fica o verbo conjugado após 'weil'? 'Ich bleibe zu Hause, weil ich krank ___.'", List.of("bin", "habe", "warst", "sein"), 0),
            new DiagnosticQuestionDTO(6, "B1", "Vocabulário", "O que significa 'eine Entscheidung treffen'?", List.of("Tomar uma decisão", "Cancelar uma viagem", "Escrever uma carta", "Fazer uma pergunta"), 0),
            new DiagnosticQuestionDTO(7, "B2", "Gramática", "Identifique a frase correta no Konjunktiv II:", List.of(
                "Wenn ich mehr Zeit hätte, würde ich mehr lesen.",
                "Wenn ich mehr Zeit habe, werde ich lesen hätte.",
                "Wenn ich mehr Zeit hatte, würde lesen ich.",
                "Wenn ich würde haben Zeit, ich lese."
            ), 0),
            new DiagnosticQuestionDTO(8, "B2", "Vocabulário", "Qual o sentido de 'allerdings' em um debate?", List.of("De maneira alguma", "Por outro lado / Contudo", "Com certeza absoluta", "Daqui a pouco"), 1)
        ),
        "Italiano", List.of(
            new DiagnosticQuestionDTO(1, "A1", "Gramática", "Complete: 'Noi ___ italiani.'", List.of("sono", "siete", "siamo", "è"), 2),
            new DiagnosticQuestionDTO(2, "A1", "Vocabulário", "Como se diz 'Bom dia' em italiano?", List.of("Buonanotte", "Buongiorno", "Arrivederci", "Prego"), 1),
            new DiagnosticQuestionDTO(3, "A2", "Gramática", "Complete no passato prossimo: 'Ieri Marco ___ una pizza.'", List.of("ha mangiato", "mangia", "mangerà", "mangiava"), 0),
            new DiagnosticQuestionDTO(4, "A2", "Vocabulário", "O que significa 'il conto, per favore'?", List.of("A entrada", "A conta, por favor", "O guardanapo", "A reserva"), 1),
            new DiagnosticQuestionDTO(5, "B1", "Gramática", "Escolha o pronome correto: 'Quanti fratelli hai? ___ ho due.'", List.of("Ci", "Ne", "Lo", "Gli"), 1),
            new DiagnosticQuestionDTO(6, "B1", "Vocabulário", "O que significa 'avere fretta'?", List.of("Ter calma", "Ter pressa", "Ter fome", "Ter frio"), 1),
            new DiagnosticQuestionDTO(7, "B2", "Gramática", "Identifique a sentença correta com congiuntivo:", List.of(
                "Penso che lui abbia ragione.",
                "Penso che lui ha ragione.",
                "Penso che lui avere ragione.",
                "Penso lui aveva ragione."
            ), 0),
            new DiagnosticQuestionDTO(8, "B2", "Vocabulário", "A expressão 'tuttavia' corresponde a:", List.of("Portanto", "Todavia / Contudo", "Somente", "Enquanto isso"), 1)
        ),
        "Japonês", List.of(
            new DiagnosticQuestionDTO(1, "A1", "Gramática", "Qual partícula marca o tópico da frase em 'Watashi ___ gakusei desu'?", List.of("wa (は)", "wo (を)", "ni (に)", "de (で)"), 0),
            new DiagnosticQuestionDTO(2, "A1", "Vocabulário", "O que significa 'Arigatou gozaimasu'?", List.of("Com licença", "Muito obrigado", "Boa noite", "Desculpe-me"), 1),
            new DiagnosticQuestionDTO(3, "A2", "Gramática", "Qual partícula indica local de ação com verbo dinâmico ('Cafe ___ benkyou shimasu')?", List.of("de (で)", "e (へ)", "to (と)", "ka (か)"), 0),
            new DiagnosticQuestionDTO(4, "A2", "Vocabulário", "O que significa 'Kudasai' ao pedir algo em um restaurante?", List.of("Por favor, me dê", "Quanto custa?", "Não obrigado", "Está fechado"), 0),
            new DiagnosticQuestionDTO(5, "B1", "Gramática", "Qual forma expressa desejo ('Quero beber água' - Nomu)?", List.of("Nomitai desu", "Nomimasen", "Nomimashita", "Nonde imasu"), 0),
            new DiagnosticQuestionDTO(6, "B1", "Vocabulário", "O que significa 'Eki' (駅)?", List.of("Aeroporto", "Estação de trem", "Escola", "Hospital"), 1),
            new DiagnosticQuestionDTO(7, "B2", "Gramática", "Qual forma indica 'ter a experiência de já ter feito algo'?", List.of("...ta koto ga arimasu", "...te kudasai", "...nakereba narimasen", "...you to omoimasu"), 0),
            new DiagnosticQuestionDTO(8, "B2", "Vocabulário", "A expressão 'Shikashi' (しかし) significa:", List.of("Por exemplo", "No entanto / Porém", "Finalmente", "Agora mesmo"), 1)
        )
    );

    public LearningPathService(
            StudentLearningPathRepository pathRepository,
            LearningPathModuleRepository moduleRepository,
            StudentModuleProgressRepository progressRepository,
            StudentRepository studentRepository,
            StudentLanguageRepository studentLanguageRepository) {
        this.pathRepository = pathRepository;
        this.moduleRepository = moduleRepository;
        this.progressRepository = progressRepository;
        this.studentRepository = studentRepository;
        this.studentLanguageRepository = studentLanguageRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedInitialModules() {
        List<LearningPathModule> toSeed = new ArrayList<>();
        seedLanguageModules(toSeed, "Inglês", List.of(
            moduleDef("Inglês", "A1", 1, "Fundamentos e Apresentação Pessoal", "Aprenda cumprimentos, verbo to be, pronomes pessoais e como se apresentar com naturalidade.", "Fundamentos", "Gramática, Vocabulário", 40),
            moduleDef("Inglês", "A1", 2, "Rotina Diária e Horários", "Estruture frases no presente simples, converse sobre hábitos, horas e tarefas do dia a dia.", "Cotidiano", "Vocabulário, Leitura", 50),
            moduleDef("Inglês", "A2", 3, "Passado Simples e Relato de Experiências", "Domine verbos regulares e irregulares no passado para contar histórias e acontecimentos.", "Gramática", "Gramática, Escrita", 60),
            moduleDef("Inglês", "A2", 4, "Planos Futuros e Previsões", "Use 'will', 'going to' e presente contínuo para agendar planos e expressar expectativas.", "Estruturas", "Gramática, Fala", 60),
            moduleDef("Inglês", "B1", 5, "Viagens, Hotéis e Emergências", "Comunique-se em aeroportos, hotéis, farmácias e resolva imprevistos em outros países.", "Viagem", "Audição, Fala", 80),
            moduleDef("Inglês", "B1", 6, "Conectivos e Expressão de Opiniões", "Aprenda conectivos de contraste, causa e efeito para defender argumentos e participar de debates.", "Comunicação", "Leitura, Escrita", 80),
            moduleDef("Inglês", "B2", 7, "Comunicação Corporativa e E-mails Profissionais", "Redija e-mails formais, conduza reuniões, apresente relatórios e negocie termos comerciais.", "Carreira", "Escrita, Fala", 100),
            moduleDef("Inglês", "B2", 8, "Nuances Idiomáticas e Phrasal Verbs", "Domine expressões idiomáticas avançadas e phrasal verbs comuns em conversas nativas.", "Avançado", "Vocabulário, Audição", 120)
        ));
        seedLanguageModules(toSeed, "Espanhol", List.of(
            moduleDef("Espanhol", "A1", 1, "Saludos, Cortesía y Presentaciones", "Primeros pasos en español, reglas de pronunciación y saludos cordiales.", "Fundamentos", "Vocabulário, Fala", 40),
            moduleDef("Espanhol", "A2", 2, "El Pasado y las Anécdotas", "Pretérito indefinido y pretérito imperfecto para contar viajes y memorias.", "Gramática", "Gramática, Leitura", 60),
            moduleDef("Espanhol", "B1", 3, "Viajes, Compras y Opiniones", "Conectores y uso práctico del subjuntivo presente en situaciones reales.", "Comunicação", "Audição, Fala", 80),
            moduleDef("Espanhol", "B2", 4, "Debates Profesionales y Matices Culturales", "Argumentación avanzada, modismos iberoamericanos y redacción formal.", "Carreira", "Escrita, Vocabulário", 100)
        ));
        seedLanguageModules(toSeed, "Francês", List.of(
            moduleDef("Francês", "A1", 1, "Bonjour! Apresentações e Sons Nasais", "Domine saudações formais/informais, verbo être/avoir e pronúncia francesa.", "Fundamentos", "Vocabulário, Fala", 40),
            moduleDef("Francês", "A2", 2, "No Café, Mercado e Passé Composé", "Faça pedidos em bistrôs e relate acontecimentos passados com clareza.", "Cotidiano", "Gramática, Audição", 60),
            moduleDef("Francês", "B1", 3, "Viagens pela França e Pronomes Y / En", "Compre passagens, converse sobre planos e domine substituições pronominais.", "Viagem", "Leitura, Fala", 80),
            moduleDef("Francês", "B2", 4, "Argumentação, Cultura e Subjuntivo", "Expresse opiniões sofisticadas, hipóteses e correspondência profissional.", "Carreira", "Escrita, Gramática", 100)
        ));
        seedLanguageModules(toSeed, "Alemão", List.of(
            moduleDef("Alemão", "A1", 1, "Hallo! Apresentação e Ordem Verbal", "Pronúncia, artigos der/die/das e conjugação regular no presente.", "Fundamentos", "Gramática, Vocabulário", 40),
            moduleDef("Alemão", "A2", 2, "Casos Acusativo/Dativo e Rotina Urbana", "Use preposições espaciais e descreva trajetos e compromissos diários.", "Cotidiano", "Gramática, Leitura", 60),
            moduleDef("Alemão", "B1", 3, "Perfekt, Subordinadas (weil/dass) e Viagens", "Narre experiências no passado e conecte orações subordinadas corretamente.", "Viagem", "Audição, Escrita", 80),
            moduleDef("Alemão", "B2", 4, "Konjunktiv II e Ambiente Corporativo", "Domine expressões de cortesia, hipóteses e reuniões técnicas em alemão.", "Carreira", "Fala, Vocabulário", 100)
        ));
        seedLanguageModules(toSeed, "Italiano", List.of(
            moduleDef("Italiano", "A1", 1, "Ciao! Fonética, Essere e Avere", "Aprenda sons do italiano, cumprimentos e frases essenciais para iniciantes.", "Fundamentos", "Vocabulário, Fala", 40),
            moduleDef("Italiano", "A2", 2, "Gastronomia, Compras e Passato Prossimo", "Peça pratos típicos, negocie compras e conte o que fez recentemente.", "Cotidiano", "Gramática, Leitura", 60),
            moduleDef("Italiano", "B1", 3, "Cidades, Transportes e Pronomes Combinados", "Viaje de trem pela Itália, dê indicações e conecte ideias com fluência.", "Viagem", "Audição, Fala", 80),
            moduleDef("Italiano", "B2", 4, "Congiuntivo e Comunicação Profissional", "Construa discursos persuasivos, opiniões refinadas e textos estruturados.", "Carreira", "Escrita, Gramática", 100)
        ));
        seedLanguageModules(toSeed, "Japonês", List.of(
            moduleDef("Japonês", "A1", 1, "Saudações, Hiragana e Partículas は/を", "Apresente-se com polidez (-desu/-masu) e monte frases básicas.", "Fundamentos", "Vocabulário, Leitura", 40),
            moduleDef("Japonês", "A2", 2, "Cotidiano, Forma -masu Passado e Locais", "Peça em restaurantes, compre itens e indique direções na cidade.", "Cotidiano", "Gramática, Audição", 60),
            moduleDef("Japonês", "B1", 3, "Forma -te, Pedidos Polidos (-te kudasai)", "Faça solicitações naturais, descreva ações contínuas e experiências.", "Comunicação", "Fala, Escrita", 80),
            moduleDef("Japonês", "B2", 4, "Keigo Essencial e Expressões de Negócios", "Domine registros formais, conectivos argumentativos e conversas de trabalho.", "Carreira", "Vocabulário, Gramática", 100)
        ));

        if (!toSeed.isEmpty()) {
            moduleRepository.saveAll(toSeed);
            log.info("Inicializados {} módulos pedagógicos faltantes na trilha CEFR (6 idiomas).", toSeed.size());
        }
    }

    private void seedLanguageModules(
            List<LearningPathModule> accumulator,
            String language,
            List<LearningPathModule> candidates) {
        List<LearningPathModule> existing = moduleRepository.findByLanguageIgnoreCaseOrderByModuleOrderAsc(language);
        var existingOrders = existing.stream()
            .map(LearningPathModule::getModuleOrder)
            .collect(Collectors.toSet());
        for (LearningPathModule candidate : candidates) {
            if (!existingOrders.contains(candidate.getModuleOrder())) {
                accumulator.add(candidate);
            }
        }
    }

    private static LearningPathModule moduleDef(
            String language,
            String cefrLevel,
            int order,
            String title,
            String description,
            String category,
            String competenciesFocused,
            int requiredXp) {
        return LearningPathModule.builder()
            .language(language)
            .cefrLevel(cefrLevel)
            .moduleOrder(order)
            .title(title)
            .description(description)
            .category(category)
            .competenciesFocused(competenciesFocused)
            .requiredXp(requiredXp)
            .build();
    }

    public List<DiagnosticQuestionDTO> getDiagnosticQuestions() {
        return getDiagnosticQuestions(SupportedLanguages.DEFAULT);
    }

    public List<DiagnosticQuestionDTO> getDiagnosticQuestions(String language) {
        String lang = SupportedLanguages.normalizeOrDefault(language, SupportedLanguages.DEFAULT);
        return DIAGNOSTIC_QUESTIONS_BY_LANGUAGE.getOrDefault(
            lang,
            DIAGNOSTIC_QUESTIONS_BY_LANGUAGE.get(SupportedLanguages.DEFAULT)
        );
    }

    @Transactional
    public DiagnosticResultDTO submitDiagnostic(Long studentId, DiagnosticAnswerDTO dto) {
        String lang = SupportedLanguages.normalizeOrDefault(dto.language(), SupportedLanguages.DEFAULT);
        List<DiagnosticQuestionDTO> questions = getDiagnosticQuestions(lang);
        Map<Integer, Integer> answers = dto.answers() != null ? dto.answers() : Map.of();

        int correctCount = 0;
        int totalQuestions = questions.size();

        for (DiagnosticQuestionDTO q : questions) {
            Integer studentAnswer = answers.get(q.id());
            if (studentAnswer != null && studentAnswer == q.correctIndex()) {
                correctCount++;
            }
        }

        double scorePercent = totalQuestions == 0 ? 0.0 : (double) correctCount / totalQuestions * 100.0;
        String level;
        double gramScore, vocScore, listScore, spkScore, readScore, writScore;

        if (correctCount >= 7) {
            level = "B2";
            gramScore = 76.0; vocScore = 82.0; listScore = 68.0; spkScore = 66.0; readScore = 84.0; writScore = 74.0;
        } else if (correctCount >= 5) {
            level = "B1";
            gramScore = 61.0; vocScore = 66.0; listScore = 54.0; spkScore = 49.0; readScore = 71.0; writScore = 58.0;
        } else if (correctCount >= 3) {
            level = "A2";
            gramScore = 41.0; vocScore = 47.0; listScore = 36.0; spkScore = 31.0; readScore = 51.0; writScore = 39.0;
        } else {
            level = "A1";
            gramScore = 22.0; vocScore = 27.0; listScore = 19.0; spkScore = 16.0; readScore = 29.0; writScore = 21.0;
        }

        // Update or create StudentLearningPath
        StudentLearningPath path = pathRepository.findByStudentIdAndLanguageIgnoreCase(studentId, lang)
                .orElseGet(() -> StudentLearningPath.builder()
                        .studentId(studentId)
                        .language(lang)
                        .dailyGoalMinutes(15)
                        .currentStreakDays(1)
                        .build()
                );

        path.setCurrentCefrLevel(level);
        path.setGrammarCompetence(gramScore);
        path.setVocabularyCompetence(vocScore);
        path.setListeningCompetence(listScore);
        path.setSpeakingCompetence(spkScore);
        path.setReadingCompetence(readScore);
        path.setWritingCompetence(writScore);
        path.setLastDiagnosticAt(LocalDateTime.now());
        path.setUpdatedAt(LocalDateTime.now());
        pathRepository.save(path);

        // Unlock appropriate modules up to determined level
        unlockModulesUpToLevel(studentId, lang, level);

        String summary = String.format(
            "Diagnóstico concluído! Você acertou %d de %d perguntas (%.1f%%). Seu nível estimado é %s.",
            correctCount, totalQuestions, scorePercent, level
        );

        List<String> nextModules = moduleRepository.findByLanguageIgnoreCaseAndCefrLevelIgnoreCaseOrderByModuleOrderAsc(lang, level)
                .stream()
                .map(LearningPathModule::getTitle)
                .toList();

        return new DiagnosticResultDTO(
            lang,
            level,
            correctCount,
            totalQuestions,
            Math.round(scorePercent * 10.0) / 10.0,
            summary,
            nextModules
        );
    }

    @Transactional
    public LearningPathDTO getStudentLearningPath(Long studentId, String language) {
        String lang = SupportedLanguages.normalizeOrDefault(language, SupportedLanguages.DEFAULT);

        StudentLearningPath path = pathRepository.findByStudentIdAndLanguageIgnoreCase(studentId, lang)
                .orElseGet(() -> {
                    StudentLearningPath newPath = StudentLearningPath.builder()
                            .studentId(studentId)
                            .language(lang)
                            .currentCefrLevel("A1")
                            .targetCefrLevel("B2")
                            .dailyGoalMinutes(15)
                            .currentStreakDays(1)
                            .overallProgressPercent(0.0)
                            .grammarCompetence(18.0)
                            .vocabularyCompetence(22.0)
                            .listeningCompetence(14.0)
                            .speakingCompetence(12.0)
                            .readingCompetence(24.0)
                            .writingCompetence(16.0)
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return pathRepository.save(newPath);
                });

        List<LearningPathModule> allModules = moduleRepository.findByLanguageIgnoreCaseOrderByModuleOrderAsc(lang);
        List<StudentModuleProgress> progresses = progressRepository.findByStudentId(studentId);
        Map<Long, StudentModuleProgress> progressMap = progresses.stream()
                .collect(Collectors.toMap(StudentModuleProgress::getModuleId, p -> p, (a, b) -> a));

        List<ModuleProgressDTO> moduleDTOs = new ArrayList<>();
        int completedCount = 0;

        for (int i = 0; i < allModules.size(); i++) {
            LearningPathModule m = allModules.get(i);
            StudentModuleProgress prog = progressMap.get(m.getId());

            String status;
            double score = 0.0;

            if (prog != null) {
                status = prog.getStatus();
                score = prog.getScorePercent();
                if ("COMPLETED".equals(status)) {
                    completedCount++;
                }
            } else if (i == 0) {
                // First module is unlocked and in progress by default
                status = "IN_PROGRESS";
            } else {
                status = "LOCKED";
            }

            moduleDTOs.add(new ModuleProgressDTO(
                m.getId(),
                m.getLanguage(),
                m.getCefrLevel(),
                m.getModuleOrder(),
                m.getTitle(),
                m.getDescription(),
                m.getCategory(),
                m.getCompetenciesFocused(),
                m.getRequiredXp(),
                status,
                score
            ));
        }

        double overallPercent = allModules.size() > 0
            ? Math.round(((double) completedCount / allModules.size() * 100.0) * 10.0) / 10.0
            : 0.0;
        boolean changed = Double.compare(path.getOverallProgressPercent(), overallPercent) != 0;
        path.setOverallProgressPercent(overallPercent);
        if (refreshCompetencesFromActivity(path, allModules, progressMap, lang)) {
            changed = true;
        }
        if (changed) {
            path.setUpdatedAt(LocalDateTime.now());
            pathRepository.save(path);
        }

        return new LearningPathDTO(
            path.getId(),
            path.getStudentId(),
            path.getLanguage(),
            path.getCurrentCefrLevel(),
            path.getTargetCefrLevel(),
            path.getDailyGoalMinutes(),
            path.getCurrentStreakDays(),
            path.getOverallProgressPercent(),
            path.getGrammarCompetence(),
            path.getVocabularyCompetence(),
            path.getListeningCompetence(),
            path.getSpeakingCompetence(),
            path.getReadingCompetence(),
            path.getWritingCompetence(),
            path.getLastDiagnosticAt(),
            moduleDTOs
        );
    }

    @Transactional
    public LearningPathDTO updateGoals(Long studentId, LearningGoalUpdateDTO dto) {
        String lang = dto.language() != null && !dto.language().isBlank() ? dto.language().trim() : "Inglês";
        StudentLearningPath path = pathRepository.findByStudentIdAndLanguageIgnoreCase(studentId, lang)
                .orElseGet(() -> StudentLearningPath.builder()
                        .studentId(studentId)
                        .language(lang)
                        .currentCefrLevel("A1")
                        .build()
                );

        path.setDailyGoalMinutes(dto.dailyGoalMinutes());
        if (dto.targetCefrLevel() != null && !dto.targetCefrLevel().isBlank()) {
            path.setTargetCefrLevel(dto.targetCefrLevel().trim());
        }
        path.setUpdatedAt(LocalDateTime.now());
        pathRepository.save(path);

        return getStudentLearningPath(studentId, lang);
    }

    @Transactional
    public LearningPathDTO completeModule(Long studentId, Long moduleId, double scorePercent) {
        LearningPathModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Módulo pedagógico não encontrado."));

        double boundedScore = Math.max(0.0, Math.min(100.0, scorePercent));
        StudentModuleProgress progress = progressRepository.findByStudentIdAndModuleId(studentId, moduleId)
                .orElseGet(() -> StudentModuleProgress.builder()
                        .studentId(studentId)
                        .moduleId(moduleId)
                        .build()
                );

        boolean alreadyCompleted = "COMPLETED".equalsIgnoreCase(progress.getStatus());
        progress.setStatus("COMPLETED");
        progress.setScorePercent(Math.max(progress.getScorePercent(), boundedScore));
        progress.setCompletedAt(LocalDateTime.now());
        progressRepository.save(progress);

        // Unlock next module
        List<LearningPathModule> modules = moduleRepository.findByLanguageIgnoreCaseOrderByModuleOrderAsc(module.getLanguage());
        int nextOrder = module.getModuleOrder() + 1;
        modules.stream()
                .filter(m -> m.getModuleOrder() == nextOrder)
                .findFirst()
                .ifPresent(nextMod -> {
                    StudentModuleProgress nextProg = progressRepository.findByStudentIdAndModuleId(studentId, nextMod.getId())
                            .orElseGet(() -> StudentModuleProgress.builder()
                                    .studentId(studentId)
                                    .moduleId(nextMod.getId())
                                    .build()
                            );
                    if ("LOCKED".equals(nextProg.getStatus())) {
                        nextProg.setStatus("IN_PROGRESS");
                        progressRepository.save(nextProg);
                    }
                });

        // Award student XP only on first completion of the module
        if (!alreadyCompleted) {
            awardExperience(studentId, module.getLanguage(), module.getRequiredXp());
        }

        return getStudentLearningPath(studentId, module.getLanguage());
    }

    public List<DiagnosticQuestionDTO> getModuleQuiz(Long moduleId) {
        LearningPathModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Módulo pedagógico não encontrado."));
        String lang = SupportedLanguages.normalizeOrDefault(module.getLanguage(), SupportedLanguages.DEFAULT);
        List<DiagnosticQuestionDTO> bank = getDiagnosticQuestions(lang);
        int start = ((Math.max(1, module.getModuleOrder()) - 1) * 2) % Math.max(1, bank.size());
        List<DiagnosticQuestionDTO> selected = new ArrayList<>();
        for (int i = 0; i < Math.min(3, bank.size()); i++) {
            DiagnosticQuestionDTO q = bank.get((start + i) % bank.size());
            selected.add(new DiagnosticQuestionDTO(
                q.id(),
                module.getCefrLevel(),
                module.getCategory(),
                q.prompt(),
                q.options(),
                q.correctIndex()
            ));
        }
        return selected;
    }

    @Transactional
    public Map<String, Object> submitModuleQuizPayload(Long studentId, Long moduleId, Map<String, Object> body) {
        List<DiagnosticQuestionDTO> quiz = getModuleQuiz(moduleId);
        Map<Integer, Integer> answers = new java.util.HashMap<>();
        if (body != null) {
            Object rawAnswers = body.get("answers");
            if (rawAnswers instanceof Map<?, ?> rawMap) {
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) {
                        continue;
                    }
                    try {
                        int qId = Integer.parseInt(entry.getKey().toString());
                        int idx = entry.getValue() instanceof Number n
                                ? n.intValue()
                                : Integer.parseInt(entry.getValue().toString());
                        answers.put(qId, idx);
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else if (body.get("selectedIndices") instanceof List<?> rawList) {
                for (int i = 0; i < Math.min(quiz.size(), rawList.size()); i++) {
                    Object val = rawList.get(i);
                    if (val == null) {
                        continue;
                    }
                    try {
                        int idx = val instanceof Number n
                                ? n.intValue()
                                : Integer.parseInt(val.toString());
                        answers.put(quiz.get(i).id(), idx);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return submitModuleQuiz(studentId, moduleId, answers);
    }

    @Transactional
    public Map<String, Object> submitModuleQuiz(Long studentId, Long moduleId, Map<Integer, Integer> answers) {
        LearningPathModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Módulo pedagógico não encontrado."));
        List<DiagnosticQuestionDTO> quiz = getModuleQuiz(moduleId);
        int correct = 0;
        for (DiagnosticQuestionDTO q : quiz) {
            Integer selected = answers != null ? answers.get(q.id()) : null;
            if (selected != null && selected == q.correctIndex()) {
                correct++;
            }
        }
        int total = Math.max(1, quiz.size());
        double scorePercent = Math.round(((double) correct / total * 100.0) * 10.0) / 10.0;
        boolean passed = scorePercent >= 60.0;
        boolean wasAlreadyCompleted = progressRepository.findByStudentIdAndModuleId(studentId, moduleId)
                .map(p -> "COMPLETED".equalsIgnoreCase(p.getStatus()))
                .orElse(false);
        int xpEarned = 0;
        LearningPathDTO path;
        if (passed) {
            path = completeModule(studentId, moduleId, scorePercent);
            xpEarned = wasAlreadyCompleted ? 0 : Math.max(0, module.getRequiredXp());
        } else {
            StudentModuleProgress progress = progressRepository.findByStudentIdAndModuleId(studentId, moduleId)
                    .orElseGet(() -> StudentModuleProgress.builder()
                            .studentId(studentId)
                            .moduleId(moduleId)
                            .status("IN_PROGRESS")
                            .build());
            if (!"COMPLETED".equalsIgnoreCase(progress.getStatus())) {
                progress.setStatus("IN_PROGRESS");
                progress.setScorePercent(Math.max(progress.getScorePercent(), scorePercent));
                progressRepository.save(progress);
            }
            path = getStudentLearningPath(studentId, module.getLanguage());
        }
        String feedback = passed
            ? String.format("Módulo concluído com %d/%d acertos (%.1f%%)! Próxima etapa desbloqueada.", correct, total, scorePercent)
            : String.format("Você acertou %d/%d (%.1f%%). É necessário atingir pelo menos 60%% para concluir este módulo.", correct, total, scorePercent);
        return Map.of(
            "passed", passed,
            "correctCount", correct,
            "totalCount", total,
            "scorePercent", scorePercent,
            "xpEarned", xpEarned,
            "feedback", feedback,
            "path", path
        );
    }

    private boolean refreshCompetencesFromActivity(
            StudentLearningPath path,
            List<LearningPathModule> modules,
            Map<Long, StudentModuleProgress> progressMap,
            String language) {
        if (path == null) return false;
        double oldSum = path.getGrammarCompetence() + path.getVocabularyCompetence()
            + path.getListeningCompetence() + path.getSpeakingCompetence()
            + path.getReadingCompetence() + path.getWritingCompetence();

        double cefrBase = switch (path.getCurrentCefrLevel() != null ? path.getCurrentCefrLevel().toUpperCase() : "A1") {
            case "C1", "C2" -> 76.0;
            case "B2" -> 62.0;
            case "B1" -> 46.0;
            case "A2" -> 30.0;
            default -> 16.0;
        };

        int xpBoost = 0;
        Student student = studentRepository.findById(path.getStudentId()).orElse(null);
        if (student != null && student.getLanguages() != null) {
            xpBoost = student.getLanguages().stream()
                .filter(l -> SupportedLanguages.matches(l.getLanguageName(), language))
                .mapToInt(l -> l.getScore() != null ? l.getScore() : 0)
                .findFirst()
                .orElse(0);
        }
        double xpDelta = Math.min(18.0, xpBoost / 25.0);

        double gram = cefrBase + 2.0 + xpDelta * 0.6;
        double voc = cefrBase + 5.0 + xpDelta * 0.8;
        double list = cefrBase - 2.0 + xpDelta * 0.5;
        double spk = cefrBase - 4.0 + xpDelta * 0.5;
        double read = cefrBase + 7.0 + xpDelta * 0.7;
        double writ = cefrBase + 0.0 + xpDelta * 0.6;

        for (LearningPathModule m : modules) {
            StudentModuleProgress p = progressMap.get(m.getId());
            if (p != null && "COMPLETED".equalsIgnoreCase(p.getStatus())) {
                String focused = SupportedLanguages.stripAccentsLower(
                    m.getCompetenciesFocused() != null ? m.getCompetenciesFocused() : ""
                );
                double bonus = 6.0 * (Math.max(60.0, p.getScorePercent()) / 100.0);
                if (focused.contains("gram")) gram += bonus; else gram += 1.5;
                if (focused.contains("vocab")) voc += bonus; else voc += 1.5;
                if (focused.contains("aud") || focused.contains("list")) list += bonus; else list += 1.2;
                if (focused.contains("fala") || focused.contains("pron") || focused.contains("speak")) spk += bonus; else spk += 1.2;
                if (focused.contains("leit") || focused.contains("read")) read += bonus; else read += 1.5;
                if (focused.contains("escrit") || focused.contains("writ")) writ += bonus; else writ += 1.5;
            }
        }

        // Preserve diagnostic highs when diagnostic was explicitly taken and higher
        path.setGrammarCompetence(clampPct(Math.max(path.getLastDiagnosticAt() != null ? path.getGrammarCompetence() : 0.0, gram)));
        path.setVocabularyCompetence(clampPct(Math.max(path.getLastDiagnosticAt() != null ? path.getVocabularyCompetence() : 0.0, voc)));
        path.setListeningCompetence(clampPct(Math.max(path.getLastDiagnosticAt() != null ? path.getListeningCompetence() : 0.0, list)));
        path.setSpeakingCompetence(clampPct(Math.max(path.getLastDiagnosticAt() != null ? path.getSpeakingCompetence() : 0.0, spk)));
        path.setReadingCompetence(clampPct(Math.max(path.getLastDiagnosticAt() != null ? path.getReadingCompetence() : 0.0, read)));
        path.setWritingCompetence(clampPct(Math.max(path.getLastDiagnosticAt() != null ? path.getWritingCompetence() : 0.0, writ)));

        double newSum = path.getGrammarCompetence() + path.getVocabularyCompetence()
            + path.getListeningCompetence() + path.getSpeakingCompetence()
            + path.getReadingCompetence() + path.getWritingCompetence();
        return Math.abs(newSum - oldSum) > 0.05;
    }

    private static double clampPct(double val) {
        return Math.round(Math.max(10.0, Math.min(100.0, val)) * 10.0) / 10.0;
    }

    private void unlockModulesUpToLevel(Long studentId, String language, String cefrLevel) {
        List<LearningPathModule> modules = moduleRepository.findByLanguageIgnoreCaseOrderByModuleOrderAsc(language);
        for (LearningPathModule m : modules) {
            if (isLevelEqualOrBelow(m.getCefrLevel(), cefrLevel)) {
                StudentModuleProgress prog = progressRepository.findByStudentIdAndModuleId(studentId, m.getId())
                        .orElseGet(() -> StudentModuleProgress.builder()
                                .studentId(studentId)
                                .moduleId(m.getId())
                                .build()
                        );
                if ("LOCKED".equals(prog.getStatus())) {
                    prog.setStatus("IN_PROGRESS");
                    progressRepository.save(prog);
                }
            }
        }
    }

    private boolean isLevelEqualOrBelow(String moduleLevel, String targetLevel) {
        List<String> order = List.of("A1", "A2", "B1", "B2", "C1", "C2");
        int idxMod = order.indexOf(moduleLevel.toUpperCase());
        int idxTgt = order.indexOf(targetLevel.toUpperCase());
        return idxMod >= 0 && idxTgt >= 0 && idxMod <= idxTgt;
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
            log.warn("Erro ao atribuir XP de trilha de aprendizagem: {}", e.getMessage());
        }
    }
}
