package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.duolinfo.ia.proj.entity.AiConversationSession;
import com.duolinfo.ia.proj.entity.ExerciseSubmission;
import com.duolinfo.ia.proj.entity.LearningExercise;
import com.duolinfo.ia.proj.entity.LearningPathModule;
import com.duolinfo.ia.proj.entity.PronunciationAttempt;
import com.duolinfo.ia.proj.entity.PronunciationChallenge;
import com.duolinfo.ia.proj.entity.SpacedReviewItem;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.entity.StudentLearningPath;
import com.duolinfo.ia.proj.entity.StudentModuleProgress;
import com.duolinfo.ia.proj.repository.AiConversationSessionRepository;
import com.duolinfo.ia.proj.repository.ExerciseSubmissionRepository;
import com.duolinfo.ia.proj.repository.LearningPathModuleRepository;
import com.duolinfo.ia.proj.repository.PronunciationAttemptRepository;
import com.duolinfo.ia.proj.repository.PronunciationChallengeRepository;
import com.duolinfo.ia.proj.repository.SpacedReviewItemRepository;
import com.duolinfo.ia.proj.repository.StudentLanguageRepository;
import com.duolinfo.ia.proj.repository.StudentLearningPathRepository;
import com.duolinfo.ia.proj.repository.StudentModuleProgressRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.StudentVocabularyRepository;
import com.duolinfo.ia.proj.util.SupportedLanguages;

@Service
public class LearningPathService {

    private static final Logger log = LoggerFactory.getLogger(LearningPathService.class);

    private final StudentLearningPathRepository pathRepository;
    private final LearningPathModuleRepository moduleRepository;
    private final StudentModuleProgressRepository progressRepository;
    private final StudentRepository studentRepository;
    private final StudentLanguageRepository studentLanguageRepository;

    @Autowired(required = false)
    private ExerciseSubmissionRepository exerciseSubmissionRepository;

    @Autowired(required = false)
    private SpacedReviewItemRepository spacedReviewItemRepository;

    @Autowired(required = false)
    private PronunciationAttemptRepository pronunciationAttemptRepository;

    @Autowired(required = false)
    private PronunciationChallengeRepository pronunciationChallengeRepository;

    @Autowired(required = false)
    private AiConversationSessionRepository aiConversationSessionRepository;

    @Autowired(required = false)
    private StudentVocabularyRepository studentVocabularyRepository;

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

    private static final Map<String, List<DiagnosticQuestionDTO>> MODULE_EXTRA_QUESTIONS_BY_LANGUAGE = Map.of(
        "Inglês", List.of(
            new DiagnosticQuestionDTO(9, "A1", "Cotidiano", "Escolha a rotina correta: 'What time ___ you usually wake up?'", List.of("does", "are", "do", "is"), 2, "Use o auxiliar 'do' com o pronome 'you' no presente simples."),
            new DiagnosticQuestionDTO(10, "A2", "Estruturas", "Complete o plano futuro: 'Look at those dark clouds! It ___ rain soon.'", List.of("is going to", "will be", "goes", "shall"), 0, "Quando há evidência visível no presente, prefira 'going to'."),
            new DiagnosticQuestionDTO(11, "B1", "Comunicação", "Qual conectivo expressa contraste em 'She was tired; ___, she finished the report.'?", List.of("therefore", "however", "moreover", "because"), 1, "'However' equivale a 'contudo / porém'."),
            new DiagnosticQuestionDTO(12, "B2", "Carreira", "Como abrir formalmente um e-mail corporativo sem saber o gênero da pessoa?", List.of("Hey dude,", "Dear Hiring Manager,", "What's up,", "Yo team,"), 1, "No contexto corporativo formal, usa-se 'Dear [Nome ou Cargo],'.")
        ),
        "Espanhol", List.of(
            new DiagnosticQuestionDTO(9, "A1", "Cotidiano", "Complete com o presente: 'Todos los días yo ___ a las siete.'", List.of("me levanto", "me levanté", "levantarme", "te levantas"), 0, "Com o sujeito 'yo', o verbo reflexivo recebe 'me' e terminação '-o'."),
            new DiagnosticQuestionDTO(10, "A2", "Estruturas", "Qual forma expressa futuro imediato: 'Este fin de semana ___ a visitar a mis abuelos.'", List.of("voy", "fui", "iba", "fuiste"), 0, "A perífrase 'ir a + infinitivo' expressa planos próximos ('voy a visitar')."),
            new DiagnosticQuestionDTO(11, "B1", "Viagem", "Em um hotel, o que significa 'Quisiera hacer el check-out'?", List.of("Reservar um quarto", "Pedir serviço de quarto", "Encerrar a hospedagem / sair", "Trocar de andar"), 2, "'Hacer el check-out' significa finalizar a estadia e pagar a conta."),
            new DiagnosticQuestionDTO(12, "B2", "Carreira", "Assinale o fechamento adequado para um e-mail profissional em espanhol:", List.of("Atentamente,", "Chao amigo,", "Nos vemos luego,", "Besitos,"), 0, "'Atentamente,' ou 'Cordialmente,' são padrões formais.")
        ),
        "Francês", List.of(
            new DiagnosticQuestionDTO(9, "A1", "Cotidiano", "Complete: 'Le matin, nous ___ du café.'", List.of("buvons", "boit", "buvez", "boivent"), 0, "Com 'nous', a terminação verbal no presente é '-ons' (buvons)."),
            new DiagnosticQuestionDTO(10, "A2", "Estruturas", "Como dizer 'Eu vou viajar amanhã' usando futur proche?", List.of("Je vais voyager demain.", "J'ai voyagé demain.", "Je voyageais demain.", "Je suis voyagé demain."), 0, "O futur proche forma-se com 'aller no presente + infinitivo'."),
            new DiagnosticQuestionDTO(11, "B1", "Viagem", "Na estação de trem, 'Un aller-retour pour Lyon' significa:", List.of("Apenas ida para Lyon", "Bilhete de ida e volta para Lyon", "Plataforma de embarque", "Atraso no trem"), 1, "'Aller-retour' = passagem de ida e volta."),
            new DiagnosticQuestionDTO(12, "B2", "Carreira", "Em uma mensagem formal em francês, qual saudação final é mais adequada?", List.of("À plus !", "Salut !", "Cordialement,", "Bisous,"), 2, "'Cordialement,' é a despedida corporativa mais usada em francês.")
        ),
        "Alemão", List.of(
            new DiagnosticQuestionDTO(9, "A1", "Cotidiano", "Qual frase está com a ordem verbal correta no presente?", List.of("Jeden Tag trinke ich Kaffee.", "Jeden Tag ich trinke Kaffee.", "Trinke jeden Tag ich Kaffee.", "Ich jeden Tag Kaffee trinke."), 0, "Em orações principais alemãs, o verbo conjugado ocupa sempre a 2ª posição."),
            new DiagnosticQuestionDTO(10, "A2", "Estruturas", "Qual preposição pede sempre dativo em 'Ich fahre ___ dem Zug'?", List.of("mit", "für", "durch", "ohne"), 0, "'mit' (com / de meio de transporte) exige sempre o caso dativo."),
            new DiagnosticQuestionDTO(11, "B1", "Comunicação", "Complete com o particípio do Perfekt: 'Wir haben das Projekt gestern ___.'", List.of("beendet", "beenden", "beendete", "zu beenden"), 0, "Verbos terminados em '-en' com prefixo inseparável 'be-' não ganham 'ge-': beendet."),
            new DiagnosticQuestionDTO(12, "B2", "Carreira", "Como iniciar um e-mail formal em alemão para pessoa desconhecida?", List.of("Hallo Leute,", "Sehr geehrte Damen und Herren,", "Hi zusammen,", "Liebe Grüße,"), 1, "'Sehr geehrte Damen und Herren,' equivale a 'Prezados(as) Senhores(as)'.")
        ),
        "Italiano", List.of(
            new DiagnosticQuestionDTO(9, "A1", "Cotidiano", "Complete: 'La mattina io ___ alle sette.'", List.of("mi sveglio", "ti svegli", "si sveglia", "ci svegliamo"), 0, "Com 'io', o pronome reflexivo é 'mi' e o verbo termina em '-o'."),
            new DiagnosticQuestionDTO(10, "A2", "Estruturas", "Qual auxiliar usar com verbo de movimento em 'Maria ___ partita ieri'?", List.of("è", "ha", "sono", "abbiamo"), 0, "Verbos de movimento como 'partire' usam 'essere' no passato prossimo."),
            new DiagnosticQuestionDTO(11, "B1", "Comunicação", "O que significa a expressão 'In bocca al lupo!'?", List.of("Tenha cuidado!", "Boa sorte!", "Estou com fome!", "Vamos jantar!"), 1, "Expressão idiomática clássica italiana que deseja 'Boa sorte!'."),
            new DiagnosticQuestionDTO(12, "B2", "Carreira", "Qual encerramento é indicado em um e-mail corporativo em italiano?", List.of("Ciao bello!", "Distinti saluti,", "A dopo!", "Baci,"), 1, "'Distinti saluti,' ou 'Cordiali saluti,' são padrões formais.")
        ),
        "Japonês", List.of(
            new DiagnosticQuestionDTO(9, "A1", "Cotidiano", "Como perguntar as horas educadamente: 'Agora são quantas horas?'", List.of("Ima nan-ji desu ka.", "Ima doko desu ka.", "Ima dare desu ka.", "Ima ikura desu ka."), 0, "'Nan-ji' (何時) significa 'que horas'; 'desu ka' marca pergunta educada."),
            new DiagnosticQuestionDTO(10, "A2", "Estruturas", "Como dizer 'Não comi' na forma polida no passado?", List.of("Tabemasen deshita", "Tabemasu", "Tabemashita", "Tabete kudasai"), 0, "Negativo passado formal termina sempre em '-masen deshita'."),
            new DiagnosticQuestionDTO(11, "B1", "Comunicação", "Como pedir a alguém que fale um pouco mais devagar em japonês?", List.of("Yukkuri hanashite kudasai.", "Hayaku kite kudasai.", "Mou kaerimasu.", "Wakarimashita."), 0, "'Yukkuri' = devagar; '-te kudasai' formula pedido polido."),
            new DiagnosticQuestionDTO(12, "B2", "Carreira", "Qual expressão polida se usa ao sair antes dos colegas no trabalho?", List.of("Osaki ni shitsurei shimasu.", "Ohayou gozaimasu.", "Itadakimasu.", "Oyasuminasai."), 0, "'Osaki ni shitsurei shimasu' expressa cortesia profissional ao se retirar.")
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
            moduleDef("Espanhol", "A1", 2, "Rutina Diaria y Horarios", "Verbos reflexivos frecuentes, horas y hábitos cotidianos.", "Cotidiano", "Vocabulário, Leitura", 50),
            moduleDef("Espanhol", "A2", 3, "El Pasado y las Anécdotas", "Pretérito indefinido y pretérito imperfecto para contar viajes y memorias.", "Gramática", "Gramática, Leitura", 60),
            moduleDef("Espanhol", "A2", 4, "Planes Futuros e Intenciones", "Perífrasis 'ir a + infinitivo' y futuro simple para metas cercanas.", "Estruturas", "Gramática, Fala", 60),
            moduleDef("Espanhol", "B1", 5, "Viajes, Compras y Restaurantes", "Pida platos, negocie cambios y resuelva imprevistos en viajes.", "Viagem", "Audição, Fala", 80),
            moduleDef("Espanhol", "B1", 6, "Opiniones y Subjuntivo Presente", "Conectores 'sin embargo', 'aunque' y uso práctico del subjuntivo.", "Comunicação", "Leitura, Escrita", 80),
            moduleDef("Espanhol", "B2", 7, "Debates Profesionales y Redacción", "Argumentación avanzada, correos formales y vocabulario corporativo.", "Carreira", "Escrita, Vocabulário", 100),
            moduleDef("Espanhol", "B2", 8, "Modismos Iberoamericanos y Fluidez", "Expresiones idiomáticas de España y Latinoamérica en contextos naturales.", "Avançado", "Vocabulário, Audição", 120)
        ));
        seedLanguageModules(toSeed, "Francês", List.of(
            moduleDef("Francês", "A1", 1, "Bonjour! Apresentações e Sons Nasais", "Domine saudações formais/informais, verbo être/avoir e pronúncia francesa.", "Fundamentos", "Vocabulário, Fala", 40),
            moduleDef("Francês", "A1", 2, "Rotina Matinal e Verbos em -er", "Construa frases do dia a dia com horários, artigos e presente regular.", "Cotidiano", "Vocabulário, Leitura", 50),
            moduleDef("Francês", "A2", 3, "No Café, Mercado e Passé Composé", "Faça pedidos em bistrôs e relate acontecimentos passados com clareza.", "Gramática", "Gramática, Audição", 60),
            moduleDef("Francês", "A2", 4, "Planos Próximos e Futur Proche", "Use 'aller + infinitif' e expressões de tempo para marcar compromissos.", "Estruturas", "Gramática, Fala", 60),
            moduleDef("Francês", "B1", 5, "Viagens pela França e Pronomes Y / En", "Compre passagens, converse sobre trajetos e domine substituições pronominais.", "Viagem", "Leitura, Fala", 80),
            moduleDef("Francês", "B1", 6, "Debates, Conectivos e Imparfait", "Compare situações, narre memórias e defenda ideias com fluidez.", "Comunicação", "Leitura, Escrita", 80),
            moduleDef("Francês", "B2", 7, "Comunicação Corporativa e E-mails", "Redija correspondência formal com 'cordialement' e apresente projetos.", "Carreira", "Escrita, Gramática", 100),
            moduleDef("Francês", "B2", 8, "Subjuntivo e Nuances Culturais", "Empregue 'il faut que', 'bien que' e expressões francesas autênticas.", "Avançado", "Vocabulário, Audição", 120)
        ));
        seedLanguageModules(toSeed, "Alemão", List.of(
            moduleDef("Alemão", "A1", 1, "Hallo! Apresentação e Ordem Verbal", "Pronúncia, artigos der/die/das e conjugação regular no presente.", "Fundamentos", "Gramática, Vocabulário", 40),
            moduleDef("Alemão", "A1", 2, "Rotina, Horas e Verbo na 2ª Posição", "Monte frases cotidianas respeitando a inversão sujeito-verbo em alemão.", "Cotidiano", "Vocabulário, Leitura", 50),
            moduleDef("Alemão", "A2", 3, "Casos Acusativo/Dativo na Cidade", "Use artigos no acusativo/dativo com preposições espaciais e trajetos.", "Gramática", "Gramática, Leitura", 60),
            moduleDef("Alemão", "A2", 4, "Perfekt e Histórias do Dia a Dia", "Domine particípios 'haben/sein + Partizip II' para contar experiências.", "Estruturas", "Gramática, Escrita", 60),
            moduleDef("Alemão", "B1", 5, "Viagens de Trem e Verbos Modais", "Resolva embarques no Hauptbahnhof com dürfen, können, müssen e sollen.", "Viagem", "Audição, Fala", 80),
            moduleDef("Alemão", "B1", 6, "Subordinadas (weil, dass, obwohl)", "Conecte argumentos complexos colocando o verbo conjugado no final.", "Comunicação", "Audição, Escrita", 80),
            moduleDef("Alemão", "B2", 7, "Konjunktiv II e Ambiente Corporativo", "Domine expressões de cortesia, hipóteses e reuniões técnicas em alemão.", "Carreira", "Fala, Vocabulário", 100),
            moduleDef("Alemão", "B2", 8, "Voz Passiva e Textos Acadêmicos", "Interprete relatórios formais, conectivos 'allerdings' e nominalizações.", "Avançado", "Leitura, Escrita", 120)
        ));
        seedLanguageModules(toSeed, "Italiano", List.of(
            moduleDef("Italiano", "A1", 1, "Ciao! Fonética, Essere e Avere", "Aprenda sons do italiano, cumprimentos e frases essenciais para iniciantes.", "Fundamentos", "Vocabulário, Fala", 40),
            moduleDef("Italiano", "A1", 2, "Rotina, Família e Verbos em -are/-ere", "Descreva seu dia, gostos pessoais e compromissos com fluidez inicial.", "Cotidiano", "Vocabulário, Leitura", 50),
            moduleDef("Italiano", "A2", 3, "Gastronomia, Compras e Passato Prossimo", "Peça pratos típicos, negocie compras e conte o que fez recentemente.", "Gramática", "Gramática, Leitura", 60),
            moduleDef("Italiano", "A2", 4, "Planos Futuros e Essere vs. Avere", "Escolha auxiliares corretos em verbos de movimento e agende planos.", "Estruturas", "Gramática, Fala", 60),
            moduleDef("Italiano", "B1", 5, "Cidades, Transportes e Pronomes Ci / Ne", "Viaje de trem pela Itália, dê indicações e use pronomes combinados.", "Viagem", "Audição, Fala", 80),
            moduleDef("Italiano", "B1", 6, "Opiniões, Imperfetto e Conectores", "Expresse concordância, contraste com 'tuttavia' e conte memórias.", "Comunicação", "Leitura, Escrita", 80),
            moduleDef("Italiano", "B2", 7, "Congiuntivo e Comunicação Profissional", "Construa discursos persuasivos, e-mails formais e textos estruturados.", "Carreira", "Escrita, Gramática", 100),
            moduleDef("Italiano", "B2", 8, "Expressões Idiomáticas e Cultura Italiana", "Domine expressões naturais como 'in bocca al lupo' e registros avançados.", "Avançado", "Vocabulário, Audição", 120)
        ));
        seedLanguageModules(toSeed, "Japonês", List.of(
            moduleDef("Japonês", "A1", 1, "Saudações, Hiragana e Partículas は/を", "Apresente-se com polidez (-desu/-masu) e monte frases básicas.", "Fundamentos", "Vocabulário, Leitura", 40),
            moduleDef("Japonês", "A1", 2, "Horários, Números e Rotina Diária", "Informe horas com 'nan-ji', conte objetos e fale sobre hábitos.", "Cotidiano", "Vocabulário, Leitura", 50),
            moduleDef("Japonês", "A2", 3, "Restaurante, Lojas e Pedidos (Kudasai)", "Peça cafés, refeições e itens em lojas com educação natural.", "Gramática", "Gramática, Audição", 60),
            moduleDef("Japonês", "A2", 4, "Passado Polido (-mashita / -masen deshita)", "Narre ações concluídas no passado e marque locais com で/に.", "Estruturas", "Gramática, Escrita", 60),
            moduleDef("Japonês", "B1", 5, "Viagens, Estações (駅) e Forma -te", "Peça orientações, conecte ações com '-te' e faça pedidos (-te kudasai).", "Viagem", "Fala, Escrita", 80),
            moduleDef("Japonês", "B1", 6, "Desejos (-tai) e Experiências (-ta koto)", "Compartilhe vontades pessoais e vivências anteriores em conversas reais.", "Comunicação", "Audição, Fala", 80),
            moduleDef("Japonês", "B2", 7, "Keigo Essencial e Etiqueta de Trabalho", "Empregue expressões corporativas como 'osaki ni shitsurei shimasu'.", "Carreira", "Vocabulário, Gramática", 100),
            moduleDef("Japonês", "B2", 8, "Conectivos Argumentativos e Fluidez Avançada", "Domine 'shikashi', justificativas formais e leitura contextual em kanji.", "Avançado", "Leitura, Vocabulário", 120)
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
        List<String> availableLanguages = resolveEnrolledLanguages(studentId);
        String defaultLang = availableLanguages.isEmpty() ? SupportedLanguages.DEFAULT : availableLanguages.get(0);
        String lang = (language != null && !language.isBlank())
                ? SupportedLanguages.normalizeOrDefault(language, defaultLang)
                : defaultLang;

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
                            .grammarCompetence(0.0)
                            .vocabularyCompetence(0.0)
                            .listeningCompetence(0.0)
                            .speakingCompetence(0.0)
                            .readingCompetence(0.0)
                            .writingCompetence(0.0)
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
            } else if (i == 0 || (i > 0 && "COMPLETED".equals(
                progressMap.containsKey(allModules.get(i - 1).getId())
                    ? progressMap.get(allModules.get(i - 1).getId()).getStatus()
                    : null))) {
                // First module or immediate successor after a completed module is unlocked
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

        Map<String, Object> activityMetrics = buildActivityMetrics(studentId, lang, allModules, progressMap, completedCount);

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
            moduleDTOs,
            availableLanguages,
            activityMetrics
        );
    }

    private List<String> resolveEnrolledLanguages(Long studentId) {
        if (studentId == null) {
            return List.of();
        }
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null || student.getLanguages() == null || student.getLanguages().isEmpty()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        for (StudentLanguage sl : student.getLanguages()) {
            if (sl == null || sl.getLanguageName() == null || sl.getLanguageName().isBlank()) {
                continue;
            }
            SupportedLanguages.normalize(sl.getLanguageName()).ifPresent(seen::add);
        }
        return List.copyOf(seen);
    }

    private Map<String, Object> buildActivityMetrics(
            Long studentId,
            String language,
            List<LearningPathModule> modules,
            Map<Long, StudentModuleProgress> progressMap,
            int modulesCompleted) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("modulesCompleted", modulesCompleted);
        metrics.put("modulesTotal", modules.size());

        int xp = 0;
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student != null && student.getLanguages() != null) {
            xp = student.getLanguages().stream()
                .filter(l -> SupportedLanguages.matches(l.getLanguageName(), language))
                .mapToInt(l -> l.getScore() != null ? l.getScore() : 0)
                .findFirst()
                .orElse(0);
        }
        metrics.put("languageXp", xp);

        int exerciseCount = 0;
        double exerciseAvgAccuracy = 0.0;
        if (exerciseSubmissionRepository != null) {
            List<ExerciseSubmission> subs = exerciseSubmissionRepository
                .findByStudent_IdOrderBySubmittedAtDesc(studentId)
                .stream()
                .filter(s -> s.getExercise() != null
                    && SupportedLanguages.matches(s.getExercise().getLanguageName(), language))
                .toList();
            exerciseCount = subs.size();
            if (!subs.isEmpty()) {
                int correct = subs.stream().mapToInt(ExerciseSubmission::getCorrectCount).sum();
                int totalQ = subs.stream().mapToInt(s -> Math.max(1, s.getQuestionCount())).sum();
                exerciseAvgAccuracy = Math.round(((correct * 100.0) / Math.max(1, totalQ)) * 10.0) / 10.0;
            }
        }
        metrics.put("exerciseSubmissions", exerciseCount);
        metrics.put("exerciseAvgAccuracy", exerciseAvgAccuracy);

        long srsReviewed = 0;
        if (spacedReviewItemRepository != null) {
            srsReviewed = spacedReviewItemRepository
                .findByStudent_IdOrderByDueDateAsc(studentId)
                .stream()
                .filter(i -> SupportedLanguages.matches(i.getLanguageName(), language) && i.getRepetitionNumber() > 0)
                .count();
        }
        metrics.put("srsReviewed", srsReviewed);

        long pronunciationAttempts = 0;
        if (pronunciationAttemptRepository != null && pronunciationChallengeRepository != null) {
            List<PronunciationAttempt> attempts = pronunciationAttemptRepository
                .findByStudentIdOrderByAttemptedAtDesc(studentId);
            if (!attempts.isEmpty()) {
                Set<Long> chIds = attempts.stream()
                    .map(PronunciationAttempt::getChallengeId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                Map<Long, PronunciationChallenge> chMap = pronunciationChallengeRepository.findAllById(chIds)
                    .stream().collect(Collectors.toMap(PronunciationChallenge::getId, c -> c));
                pronunciationAttempts = attempts.stream()
                    .filter(a -> {
                        PronunciationChallenge c = chMap.get(a.getChallengeId());
                        return c != null && SupportedLanguages.matches(c.getLanguage(), language);
                    })
                    .count();
            }
        }
        metrics.put("pronunciationAttempts", pronunciationAttempts);

        int aiTurns = 0;
        if (aiConversationSessionRepository != null) {
            aiTurns = aiConversationSessionRepository
                .findByStudentIdOrderByStartedAtDesc(studentId)
                .stream()
                .filter(c -> SupportedLanguages.matches(c.getTargetLanguage(), language) && c.getTotalTurns() != null)
                .mapToInt(AiConversationSession::getTotalTurns)
                .sum();
        }
        metrics.put("aiConversationTurns", aiTurns);

        long vocabularyItems = 0;
        if (studentVocabularyRepository != null) {
            vocabularyItems = studentVocabularyRepository
                .findByStudent_IdOrderByCreatedAtDesc(studentId)
                .stream()
                .filter(v -> SupportedLanguages.matches(v.getLanguage(), language))
                .count();
        }
        metrics.put("vocabularyItems", vocabularyItems);
        return metrics;
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
        List<DiagnosticQuestionDTO> fullBank = new ArrayList<>(getDiagnosticQuestions(lang));
        List<DiagnosticQuestionDTO> extras = MODULE_EXTRA_QUESTIONS_BY_LANGUAGE.getOrDefault(
            lang,
            MODULE_EXTRA_QUESTIONS_BY_LANGUAGE.getOrDefault(SupportedLanguages.DEFAULT, List.of())
        );
        fullBank.addAll(extras);

        String targetLevel = module.getCefrLevel() != null ? module.getCefrLevel().trim().toUpperCase() : "A1";
        List<DiagnosticQuestionDTO> levelMatched = fullBank.stream()
            .filter(q -> targetLevel.equalsIgnoreCase(q.cefrLevel()))
            .collect(Collectors.toCollection(ArrayList::new));

        List<DiagnosticQuestionDTO> orderedCandidates = new ArrayList<>(levelMatched);
        for (DiagnosticQuestionDTO q : fullBank) {
            if (!orderedCandidates.contains(q)) {
                orderedCandidates.add(q);
            }
        }

        int questionCount = Math.min(4, orderedCandidates.size());
        int offset = ((Math.max(1, module.getModuleOrder()) - 1) * 2) % Math.max(1, orderedCandidates.size());
        List<DiagnosticQuestionDTO> selected = new ArrayList<>();
        for (int i = 0; i < questionCount; i++) {
            DiagnosticQuestionDTO q = orderedCandidates.get((offset + i) % orderedCandidates.size());
            String hint = (q.mascotHelpMessage() != null && !q.mascotHelpMessage().isBlank())
                ? q.mascotHelpMessage()
                : String.format("Dica do Bode Branco (%s • %s): concentre-se em %s para responder com precisão.", lang, module.getCefrLevel(), module.getCategory());
            selected.add(new DiagnosticQuestionDTO(
                q.id(),
                module.getCefrLevel(),
                module.getCategory(),
                q.prompt(),
                q.options(),
                q.correctIndex(),
                hint
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
        List<Map<String, Object>> questionFeedback = new ArrayList<>();
        for (int i = 0; i < quiz.size(); i++) {
            DiagnosticQuestionDTO q = quiz.get(i);
            Integer selected = answers != null ? answers.get(q.id()) : null;
            boolean isCorrect = selected != null && selected == q.correctIndex();
            if (isCorrect) {
                correct++;
            }
            String studentAnswerText = (selected != null && selected >= 0 && selected < q.options().size())
                ? q.options().get(selected)
                : "";
            String correctAnswerText = (q.correctIndex() >= 0 && q.correctIndex() < q.options().size())
                ? q.options().get(q.correctIndex())
                : "";
            questionFeedback.add(Map.of(
                "questionIndex", i,
                "status", isCorrect ? "CORRECT" : "WRONG",
                "isCorrect", isCorrect,
                "studentAnswer", studentAnswerText,
                "correctAnswer", correctAnswerText
            ));
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
            "questionFeedback", questionFeedback,
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

        // Compute base scores realistically: untouched beginner languages start near zero
        // unless explicit diagnostic or StudentLanguage proficiency indicates otherwise.
        String declaredLevel = "";
        if (student != null && student.getLanguages() != null) {
            declaredLevel = student.getLanguages().stream()
                .filter(l -> SupportedLanguages.matches(l.getLanguageName(), language))
                .map(l -> SupportedLanguages.stripAccentsLower(l.getLevel() != null ? l.getLevel() : ""))
                .findFirst()
                .orElse("");
        }
        double levelSeed = 0.0;
        if (path.getLastDiagnosticAt() != null) {
            levelSeed = cefrBase;
        } else if (declaredLevel.contains("avanc") || declaredLevel.contains("flu")) {
            levelSeed = 28.0;
        } else if (declaredLevel.contains("interm")) {
            levelSeed = 14.0;
        } else if (xpBoost > 0) {
            levelSeed = 6.0;
        }

        double gram = levelSeed + xpDelta * 0.65;
        double voc = levelSeed + xpDelta * 0.85;
        double list = levelSeed + xpDelta * 0.55;
        double spk = levelSeed + xpDelta * 0.50;
        double read = levelSeed + xpDelta * 0.75;
        double writ = levelSeed + xpDelta * 0.60;

        for (LearningPathModule m : modules) {
            StudentModuleProgress p = progressMap.get(m.getId());
            if (p != null && ("COMPLETED".equalsIgnoreCase(p.getStatus())
                    || (p.getScorePercent() != null && p.getScorePercent() > 0))) {
                boolean completed = "COMPLETED".equalsIgnoreCase(p.getStatus());
                String focused = SupportedLanguages.stripAccentsLower(
                    m.getCompetenciesFocused() != null ? m.getCompetenciesFocused() : ""
                );
                double scoreFactor = Math.max(40.0, p.getScorePercent() != null ? p.getScorePercent() : 0.0) / 100.0;
                double bonus = (completed ? 9.0 : 3.5) * scoreFactor;
                double ambient = completed ? 2.2 * scoreFactor : 0.6 * scoreFactor;
                if (focused.contains("gram")) gram += bonus; else gram += ambient;
                if (focused.contains("vocab")) voc += bonus; else voc += ambient;
                if (focused.contains("aud") || focused.contains("list")) list += bonus; else list += ambient * 0.85;
                if (focused.contains("fala") || focused.contains("pron") || focused.contains("speak")) spk += bonus; else spk += ambient * 0.85;
                if (focused.contains("leit") || focused.contains("read")) read += bonus; else read += ambient;
                if (focused.contains("escrit") || focused.contains("writ")) writ += bonus; else writ += ambient;
            }
        }

        // Classroom Exercise Submissions in this language
        if (exerciseSubmissionRepository != null) {
            List<ExerciseSubmission> subs = exerciseSubmissionRepository
                .findByStudent_IdOrderBySubmittedAtDesc(path.getStudentId())
                .stream()
                .filter(s -> s.getExercise() != null
                    && SupportedLanguages.matches(s.getExercise().getLanguageName(), language))
                .toList();
            double subGram = 0, subVoc = 0, subList = 0, subSpk = 0, subRead = 0, subWrit = 0;
            for (ExerciseSubmission s : subs) {
                int qCount = Math.max(1, s.getQuestionCount());
                double acc = Math.max(0.25, Math.min(1.0, (double) s.getCorrectCount() / qCount));
                LearningExercise ex = s.getExercise();
                String meta = SupportedLanguages.stripAccentsLower(
                    (ex.getCategory() != null ? ex.getCategory() + " " : "")
                    + (ex.getTitle() != null ? ex.getTitle() + " " : "")
                    + (ex.getTheme() != null ? ex.getTheme() + " " : "")
                    + (ex.getQuestionsJson() != null ? ex.getQuestionsJson() : "")
                );
                double baseEx = 7.5 * acc;
                subRead += baseEx * 0.85;
                subVoc += baseEx * 0.75;
                subGram += baseEx * 0.65;
                subWrit += baseEx * 0.55;
                if (meta.contains("gram") || meta.contains("verb") || meta.contains("conjug") || meta.contains("synt")) subGram += baseEx;
                if (meta.contains("vocab") || meta.contains("palavr") || meta.contains("flash")) subVoc += baseEx;
                if (meta.contains("aud") || meta.contains("list") || meta.contains("ouvir") || meta.contains("escut")) subList += baseEx * 1.15;
                if (meta.contains("fala") || meta.contains("pron") || meta.contains("speak") || meta.contains("oral")) subSpk += baseEx * 1.15;
                if (meta.contains("leit") || meta.contains("read") || meta.contains("text") || meta.contains("compre")) subRead += baseEx;
                if (meta.contains("escrit") || meta.contains("writ") || meta.contains("open_text") || meta.contains("diss")) subWrit += baseEx;
            }
            gram += Math.min(28.0, subGram);
            voc += Math.min(28.0, subVoc);
            list += Math.min(24.0, subList);
            spk += Math.min(24.0, subSpk);
            read += Math.min(28.0, subRead);
            writ += Math.min(28.0, subWrit);
        }

        // SRS Spaced Review Flashcards in this language
        if (spacedReviewItemRepository != null) {
            List<SpacedReviewItem> srsItems = spacedReviewItemRepository
                .findByStudent_IdOrderByDueDateAsc(path.getStudentId())
                .stream()
                .filter(i -> SupportedLanguages.matches(i.getLanguageName(), language))
                .toList();
            long reviewedCount = srsItems.stream().filter(i -> i.getRepetitionNumber() > 0).count();
            long masteredCount = srsItems.stream().filter(i -> i.getRepetitionNumber() >= 2).count();
            if (reviewedCount > 0) {
                voc += Math.min(22.0, reviewedCount * 3.0 + masteredCount * 2.0);
                read += Math.min(12.0, reviewedCount * 1.8);
                list += Math.min(8.0, masteredCount * 1.5);
            }
        }

        // Pronunciation practice in this language
        if (pronunciationAttemptRepository != null && pronunciationChallengeRepository != null) {
            List<PronunciationAttempt> attempts = pronunciationAttemptRepository
                .findByStudentIdOrderByAttemptedAtDesc(path.getStudentId());
            if (!attempts.isEmpty()) {
                java.util.Set<Long> chIds = attempts.stream()
                    .map(PronunciationAttempt::getChallengeId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
                Map<Long, PronunciationChallenge> chMap = pronunciationChallengeRepository.findAllById(chIds)
                    .stream().collect(java.util.stream.Collectors.toMap(PronunciationChallenge::getId, c -> c));
                List<PronunciationAttempt> langAttempts = attempts.stream()
                    .filter(a -> {
                        PronunciationChallenge c = chMap.get(a.getChallengeId());
                        return c != null && SupportedLanguages.matches(c.getLanguage(), language);
                    })
                    .toList();
                if (!langAttempts.isEmpty()) {
                    double avgScore = langAttempts.stream()
                        .mapToDouble(PronunciationAttempt::getFinalScore).average().orElse(60.0);
                    double pFactor = Math.min(26.0, langAttempts.size() * 6.0 * (avgScore / 100.0));
                    spk += pFactor;
                    list += pFactor * 0.55;
                }
            }
        }

        // AI Conversation sessions in this language
        if (aiConversationSessionRepository != null) {
            List<AiConversationSession> convs = aiConversationSessionRepository
                .findByStudentIdOrderByStartedAtDesc(path.getStudentId())
                .stream()
                .filter(c -> SupportedLanguages.matches(c.getTargetLanguage(), language) && c.getTotalTurns() != null && c.getTotalTurns() > 0)
                .toList();
            int totalTurns = convs.stream().mapToInt(AiConversationSession::getTotalTurns).sum();
            if (totalTurns > 0) {
                spk += Math.min(14.0, totalTurns * 2.2);
                writ += Math.min(16.0, totalTurns * 2.5);
                voc += Math.min(10.0, totalTurns * 1.5);
                gram += Math.min(12.0, totalTurns * 1.8);
            }
        }

        // Personal saved vocabulary in this language
        if (studentVocabularyRepository != null) {
            long vocabCount = studentVocabularyRepository
                .findByStudent_IdOrderByCreatedAtDesc(path.getStudentId())
                .stream()
                .filter(v -> SupportedLanguages.matches(v.getLanguage(), language))
                .count();
            if (vocabCount > 0) {
                voc += Math.min(14.0, vocabCount * 2.0);
                read += Math.min(6.0, vocabCount * 0.9);
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
        return Math.round(Math.max(0.0, Math.min(100.0, val)) * 10.0) / 10.0;
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
