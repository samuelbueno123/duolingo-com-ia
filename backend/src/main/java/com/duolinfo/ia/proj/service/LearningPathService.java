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

@Service
public class LearningPathService {

    private static final Logger log = LoggerFactory.getLogger(LearningPathService.class);

    private final StudentLearningPathRepository pathRepository;
    private final LearningPathModuleRepository moduleRepository;
    private final StudentModuleProgressRepository progressRepository;
    private final StudentRepository studentRepository;
    private final StudentLanguageRepository studentLanguageRepository;

    private static final List<DiagnosticQuestionDTO> DIAGNOSTIC_QUESTIONS = List.of(
        new DiagnosticQuestionDTO(
            1, "A1", "Gramática",
            "Complete a frase: 'She ___ to the gym every morning.'",
            List.of("go", "goes", "is go", "going"),
            1
        ),
        new DiagnosticQuestionDTO(
            2, "A1", "Vocabulário",
            "Qual é o oposto da palavra 'expensive'?",
            List.of("cheap", "fast", "heavy", "difficult"),
            0
        ),
        new DiagnosticQuestionDTO(
            3, "A2", "Gramática",
            "Complete com o passado correto: 'Yesterday we ___ an interesting movie.'",
            List.of("watch", "watched", "did watched", "have watch"),
            1
        ),
        new DiagnosticQuestionDTO(
            4, "A2", "Vocabulário",
            "O que significa 'airport gate'?",
            List.of("Pista de pouso", "Portão de embarque", "Esteira de bagagens", "Guichê da alfândega"),
            1
        ),
        new DiagnosticQuestionDTO(
            5, "B1", "Gramática",
            "Escolha a opção correta: 'If it rains tomorrow, we ___ stay at home.'",
            List.of("would", "will", "are", "have"),
            1
        ),
        new DiagnosticQuestionDTO(
            6, "B1", "Vocabulário",
            "Complete com a preposição correta: 'I am really looking forward ___ meeting your family.'",
            List.of("for", "to", "at", "with"),
            1
        ),
        new DiagnosticQuestionDTO(
            7, "B2", "Gramática",
            "Identifique a sentença correta:",
            List.of(
                "Had I known about the schedule change, I would have notified you.",
                "If I would know about the schedule change, I told you.",
                "Had I knew about the schedule change, I had told you.",
                "If I had been known about the change, I will notify you."
            ),
            0
        ),
        new DiagnosticQuestionDTO(
            8, "B2", "Vocabulário",
            "O termo 'cutting-edge technology' é melhor traduzido como:",
            List.of("Tecnologia ultrapassada", "Tecnologia de ponta / inovadora", "Tecnologia de baixo custo", "Tecnologia arriscada"),
            1
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
        if (moduleRepository.count() > 0) {
            return;
        }

        List<LearningPathModule> modules = List.of(
            // Inglês
            LearningPathModule.builder()
                .language("Inglês")
                .cefrLevel("A1")
                .moduleOrder(1)
                .title("Fundamentos e Apresentação Pessoal")
                .description("Aprenda cumprimentos, verbo to be, pronomes pessoais e como se apresentar com naturalidade.")
                .category("Fundamentos")
                .competenciesFocused("Gramática, Vocabulário")
                .requiredXp(40)
                .build(),

            LearningPathModule.builder()
                .language("Inglês")
                .cefrLevel("A1")
                .moduleOrder(2)
                .title("Rotina Diária e Horários")
                .description("Estruture frases no presente simples, converse sobre hábitos, horas e tarefas do dia a dia.")
                .category("Cotidiano")
                .competenciesFocused("Vocabulário, Leitura")
                .requiredXp(50)
                .build(),

            LearningPathModule.builder()
                .language("Inglês")
                .cefrLevel("A2")
                .moduleOrder(3)
                .title("Passado Simples e Relato de Experiências")
                .description("Domine verbos regulares e irregulares no passado para contar histórias e acontecimentos.")
                .category("Gramática")
                .competenciesFocused("Gramática, Escrita")
                .requiredXp(60)
                .build(),

            LearningPathModule.builder()
                .language("Inglês")
                .cefrLevel("A2")
                .moduleOrder(4)
                .title("Planos Futuros e Previsões")
                .description("Use 'will', 'going to' e presente contínuo para agendar planos e expressar expectativas.")
                .category("Estruturas")
                .competenciesFocused("Gramática, Fala")
                .requiredXp(60)
                .build(),

            LearningPathModule.builder()
                .language("Inglês")
                .cefrLevel("B1")
                .moduleOrder(5)
                .title("Viagens, Hotéis e Emergências")
                .description("Comunique-se em aeroportos, hotéis, farmácias e resolva imprevistos em outros países.")
                .category("Viagem")
                .competenciesFocused("Audição, Fala")
                .requiredXp(80)
                .build(),

            LearningPathModule.builder()
                .language("Inglês")
                .cefrLevel("B1")
                .moduleOrder(6)
                .title("Conectivos e Expressão de Opiniões")
                .description("Aprenda conectivos de contraste, causa e efeito para defender argumentos e participar de debates.")
                .category("Comunicação")
                .competenciesFocused("Leitura, Escrita")
                .requiredXp(80)
                .build(),

            LearningPathModule.builder()
                .language("Inglês")
                .cefrLevel("B2")
                .moduleOrder(7)
                .title("Comunicação Corporativa e E-mails Profissionais")
                .description("Redija e-mails formais, conduza reuniões, apresente relatórios e negocie termos comerciais.")
                .category("Carreira")
                .competenciesFocused("Escrita, Fala")
                .requiredXp(100)
                .build(),

            LearningPathModule.builder()
                .language("Inglês")
                .cefrLevel("B2")
                .moduleOrder(8)
                .title("Nuances Idiomáticas e Phrasal Verbs")
                .description("Domine expressões idiomáticas avançadas e phrasal verbs comuns em conversas nativas.")
                .category("Avançado")
                .competenciesFocused("Vocabulário, Audição")
                .requiredXp(120)
                .build(),

            // Espanhol
            LearningPathModule.builder()
                .language("Espanhol")
                .cefrLevel("A1")
                .moduleOrder(1)
                .title("Saludos, Cortesía y Presentaciones")
                .description("Primeros pasos en el idioma español, reglas de pronunciación y saludos cordiales.")
                .category("Fundamentos")
                .competenciesFocused("Vocabulario, Pronunciación")
                .requiredXp(40)
                .build(),

            LearningPathModule.builder()
                .language("Espanhol")
                .cefrLevel("A2")
                .moduleOrder(2)
                .title("El Pasado y las Anécdotas")
                .description("Pretérito indefinido y pretérito imperfecto para contar viajes y memorias.")
                .category("Gramática")
                .competenciesFocused("Gramática, Lectura")
                .requiredXp(60)
                .build()
        );

        moduleRepository.saveAll(modules);
        log.info("Inicializados {} módulos pedagógicos na trilha de aprendizagem CEFR.", modules.size());
    }

    public List<DiagnosticQuestionDTO> getDiagnosticQuestions() {
        return DIAGNOSTIC_QUESTIONS;
    }

    @Transactional
    public DiagnosticResultDTO submitDiagnostic(Long studentId, DiagnosticAnswerDTO dto) {
        String lang = dto.language() != null && !dto.language().isBlank() ? dto.language().trim() : "Inglês";
        Map<Integer, Integer> answers = dto.answers();

        int correctCount = 0;
        int totalQuestions = DIAGNOSTIC_QUESTIONS.size();

        for (DiagnosticQuestionDTO q : DIAGNOSTIC_QUESTIONS) {
            Integer studentAnswer = answers.get(q.id());
            if (studentAnswer != null && studentAnswer == q.correctIndex()) {
                correctCount++;
            }
        }

        double scorePercent = (double) correctCount / totalQuestions * 100.0;
        String level;
        double gramScore, vocScore, listScore, spkScore, readScore, writScore;

        if (correctCount >= 7) {
            level = "B2";
            gramScore = 75.0; vocScore = 80.0; listScore = 70.0; spkScore = 70.0; readScore = 85.0; writScore = 75.0;
        } else if (correctCount >= 5) {
            level = "B1";
            gramScore = 60.0; vocScore = 65.0; listScore = 55.0; spkScore = 50.0; readScore = 70.0; writScore = 60.0;
        } else if (correctCount >= 3) {
            level = "A2";
            gramScore = 40.0; vocScore = 45.0; listScore = 35.0; spkScore = 30.0; readScore = 50.0; writScore = 40.0;
        } else {
            level = "A1";
            gramScore = 20.0; vocScore = 25.0; listScore = 20.0; spkScore = 15.0; readScore = 30.0; writScore = 20.0;
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
        String lang = (language != null && !language.isBlank()) ? language.trim() : "Inglês";

        StudentLearningPath path = pathRepository.findByStudentIdAndLanguageIgnoreCase(studentId, lang)
                .orElseGet(() -> {
                    StudentLearningPath newPath = StudentLearningPath.builder()
                            .studentId(studentId)
                            .language(lang)
                            .currentCefrLevel("A1")
                            .targetCefrLevel("B2")
                            .dailyGoalMinutes(15)
                            .currentStreakDays(1)
                            .overallProgressPercent(12.5)
                            .grammarCompetence(25.0)
                            .vocabularyCompetence(30.0)
                            .listeningCompetence(20.0)
                            .speakingCompetence(20.0)
                            .readingCompetence(35.0)
                            .writingCompetence(25.0)
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

        double overallPercent = allModules.size() > 0 ? (double) completedCount / allModules.size() * 100.0 : 0.0;
        if (overallPercent > path.getOverallProgressPercent()) {
            path.setOverallProgressPercent(Math.round(overallPercent * 10.0) / 10.0);
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

        StudentModuleProgress progress = progressRepository.findByStudentIdAndModuleId(studentId, moduleId)
                .orElseGet(() -> StudentModuleProgress.builder()
                        .studentId(studentId)
                        .moduleId(moduleId)
                        .build()
                );

        progress.setStatus("COMPLETED");
        progress.setScorePercent(Math.max(progress.getScorePercent(), scorePercent));
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

        // Award student XP
        awardExperience(studentId, module.getLanguage(), module.getRequiredXp());

        return getStudentLearningPath(studentId, module.getLanguage());
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
            log.warn("Erro ao atribuir XP de trilha de aprendizagem: {}", e.getMessage());
        }
    }
}
