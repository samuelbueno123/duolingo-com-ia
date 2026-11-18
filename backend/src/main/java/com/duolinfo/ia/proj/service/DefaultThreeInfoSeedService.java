package com.duolinfo.ia.proj.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.dto.request.ExerciseQuestionRequestDTO;
import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.LearningExercise;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.LearningExerciseRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.SchoolRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.repository.UserRepository;
import com.duolinfo.ia.proj.util.SupportedLanguages;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * Garante de forma idempotente a turma padrão "3 INFO" atribuída ao professor
 * admin@duolingo.com com um acervo inicial de 7 atividades (3 questões cada e estilos variados)
 * em qualquer banco ou ambiente onde a aplicação seja iniciada.
 */
@Service
@RequiredArgsConstructor
public class DefaultThreeInfoSeedService {

    public static final String DEFAULT_CLASS_NAME = "3 INFO";
    public static final String DEFAULT_ADMIN_EMAIL = "admin@duolingo.com";

    private static final Logger log = LoggerFactory.getLogger(DefaultThreeInfoSeedService.class);

    private final InstitutionRepository institutionRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final ClassTeacherAssignmentRepository assignmentRepository;
    private final LearningExerciseRepository exerciseRepository;
    private final TeacherService teacherService;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    private record ExerciseSeedSpec(
        String title,
        String category,
        String theme,
        String proficiencyLevel,
        String description,
        String contentBody,
        String mascotHelpMessage,
        List<ExerciseQuestionRequestDTO> questions
    ) {}

    @Transactional
    public void ensureDefaultThreeInfoSetup(Long defaultInstitutionId) {
        Institution inst = defaultInstitutionId != null
            ? institutionRepository.findById(defaultInstitutionId).orElse(null)
            : institutionRepository.findByCode(InstitutionService.DEFAULT_INSTITUTION_CODE).orElse(null);
        if (inst == null) {
            return;
        }

        Teacher adminTeacher = ensureAdminAsTeacher(inst);
        if (adminTeacher == null) {
            return;
        }

        School defaultSchool = resolveOrCreateDefaultSchool(inst);
        SchoolClass threeInfoClass = resolveOrCreateThreeInfoClass(inst, defaultSchool);
        ensureTeacherAssigned(threeInfoClass, adminTeacher);
        seedCuratedExercises(threeInfoClass, adminTeacher);
    }

    private Teacher ensureAdminAsTeacher(Institution inst) {
        User adminUser = userRepository.findByEmailIgnoreCase(DEFAULT_ADMIN_EMAIL).orElse(null);
        if (adminUser == null || adminUser.getId() == null) {
            return null;
        }

        Teacher adminTeacher = teacherRepository.findById(adminUser.getId()).orElse(null);
        if (adminTeacher == null) {
            teacherService.ensureAdminTeacherProfile(adminUser.getId());
            entityManager.flush();
            entityManager.clear();
            adminTeacher = teacherRepository.findById(adminUser.getId()).orElse(null);
        }
        if (adminTeacher == null) {
            return null;
        }

        boolean changed = false;
        if (adminTeacher.getInstitution() == null) {
            adminTeacher.setInstitution(inst);
            changed = true;
        }
        if (!Boolean.TRUE.equals(adminTeacher.getApproved())) {
            adminTeacher.setApproved(true);
            adminTeacher.setApprovalStatus("APPROVED");
            changed = true;
        }
        if (adminTeacher.getTaughtLanguages().isEmpty()) {
            adminTeacher.addTaughtLanguage("Inglês");
            adminTeacher.addTaughtLanguage("Espanhol");
            changed = true;
        }
        if (adminTeacher.getSpecializationAreas().isEmpty()) {
            adminTeacher.addSpecializationArea("Tecnologia & Idiomas");
            adminTeacher.addSpecializationArea("Gramática & Conversação");
            changed = true;
        }
        if (adminTeacher.getBibliography() == null || adminTeacher.getBibliography().isBlank()) {
            adminTeacher.setBibliography("Professor Responsável e Administrador Geral da plataforma Duolinfo.");
            changed = true;
        }
        if (changed) {
            adminTeacher = teacherRepository.save(adminTeacher);
        }
        return adminTeacher;
    }

    private School resolveOrCreateDefaultSchool(Institution inst) {
        List<School> existing = schoolRepository.findByInstitutionId(inst.getId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        String candidateInep = "INEP-" + inst.getId();
        if (schoolRepository.existsByInepCode(candidateInep)) {
            candidateInep = null;
        }
        return schoolRepository.save(School.builder()
            .name("CEFET/RJ - Unidade Nova Iguaçu")
            .inepCode(candidateInep)
            .institution(inst)
            .build());
    }

    private SchoolClass resolveOrCreateThreeInfoClass(Institution inst, School school) {
        List<SchoolClass> bySchool = schoolClassRepository.findBySchoolId(school.getId());
        for (SchoolClass sc : bySchool) {
            if (sc.getName() != null && DEFAULT_CLASS_NAME.equalsIgnoreCase(sc.getName().trim())) {
                return normalizeThreeInfoClass(sc, inst);
            }
        }
        for (SchoolClass sc : schoolClassRepository.findAll()) {
            Long scInstId = sc.getInstitution() != null
                ? sc.getInstitution().getId()
                : (sc.getSchool() != null && sc.getSchool().getInstitution() != null
                    ? sc.getSchool().getInstitution().getId()
                    : null);
            if (sc.getName() != null
                    && DEFAULT_CLASS_NAME.equalsIgnoreCase(sc.getName().trim())
                    && Objects.equals(scInstId, inst.getId())) {
                return normalizeThreeInfoClass(sc, inst);
            }
        }
        return schoolClassRepository.save(SchoolClass.builder()
            .name(DEFAULT_CLASS_NAME)
            .schoolYear("2026")
            .shift("MANHÃ")
            .gradeLevel("3º Ano Técnico em Informática")
            .languageName("Inglês")
            .school(school)
            .institution(inst)
            .active(true)
            .build());
    }

    private SchoolClass normalizeThreeInfoClass(SchoolClass sc, Institution inst) {
        boolean changed = false;
        if (sc.getInstitution() == null) {
            sc.setInstitution(inst);
            changed = true;
        }
        if (sc.getLanguageName() == null || sc.getLanguageName().isBlank()) {
            sc.setLanguageName("Inglês");
            changed = true;
        }
        if (!Boolean.TRUE.equals(sc.getActive())) {
            sc.setActive(true);
            changed = true;
        }
        return changed ? schoolClassRepository.save(sc) : sc;
    }

    private void ensureTeacherAssigned(SchoolClass schoolClass, Teacher teacher) {
        boolean alreadyAssigned = assignmentRepository.findByTeacherId(teacher.getId()).stream()
            .anyMatch(a -> a.getSchoolClass() != null
                && Objects.equals(a.getSchoolClass().getId(), schoolClass.getId())
                && a.getEndDate() == null);
        if (!alreadyAssigned) {
            assignmentRepository.save(ClassTeacherAssignment.builder()
                .schoolClass(schoolClass)
                .teacher(teacher)
                .startDate(LocalDate.now())
                .reason("Professor Responsável (Turma Padrão 3 INFO)")
                .build());
        }
    }

    private void seedCuratedExercises(SchoolClass threeInfoClass, Teacher adminTeacher) {
        String classLanguage = SupportedLanguages.normalizeOrDefault(threeInfoClass.getLanguageName(), "Inglês");
        List<LearningExercise> existingExercises = exerciseRepository
            .findBySchoolClass_IdOrderByCreatedAtDesc(threeInfoClass.getId());
        Set<String> existingTitles = existingExercises.stream()
            .map(LearningExercise::getTitle)
            .filter(Objects::nonNull)
            .map(String::trim)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        List<ExerciseSeedSpec> specs = buildDefaultThreeInfoSpecs();
        Instant baseTime = Instant.now().minusSeconds(specs.size() * 10L);
        int inserted = 0;

        for (int i = 0; i < specs.size(); i++) {
            ExerciseSeedSpec spec = specs.get(i);
            if (existingTitles.contains(spec.title().toLowerCase())) {
                continue;
            }
            try {
                LearningExercise ex = new LearningExercise();
                ex.setTitle(spec.title());
                ex.setCategory(spec.category());
                ex.setTheme(spec.theme());
                ex.setLanguageName(classLanguage);
                ex.setProficiencyLevel(spec.proficiencyLevel());
                ex.setDescription(spec.description());
                ex.setContentBody(spec.contentBody());
                ex.setMascotHelpMessage(spec.mascotHelpMessage());
                ex.setDueDate(LocalDate.now().plusDays(30));
                ex.setQuestionsJson(objectMapper.writeValueAsString(spec.questions()));
                ex.setActive(true);
                ex.setCreatedAt(baseTime.plusSeconds(i * 10L));
                ex.setTeacher(adminTeacher);
                ex.setSchoolClass(threeInfoClass);
                exerciseRepository.save(ex);
                inserted++;
            } catch (Exception ex) {
                log.warn("Falha ao criar atividade padrão '{}' para 3 INFO: {}", spec.title(), ex.getMessage());
            }
        }

        if (inserted > 0) {
            log.info("Turma padrão '3 INFO' inicializada com {} atividade(s) para {}.", inserted, DEFAULT_ADMIN_EMAIL);
        }
    }

    private static List<ExerciseSeedSpec> buildDefaultThreeInfoSpecs() {
        return List.of(
            new ExerciseSeedSpec(
                "01. Tech Vocabulary & Software Fundamentals",
                "Vocabulário",
                "Informática e Desenvolvimento",
                "Fácil",
                "Vocabulário técnico essencial para alunos de Informática em múltiplas mecânicas.",
                "Nesta atividade inicial da turma 3 INFO, revise termos fundamentais de programação, implantação e bancos de dados.",
                "Leia com atenção os pares técnicos: termos em inglês de TI aparecem muito em documentações!",
                List.of(
                    new ExerciseQuestionRequestDTO(
                        "Múltipla Escolha",
                        "No contexto de engenharia de software, o que significa realizar um 'deploy'?",
                        List.of(
                            "Apagar o código-fonte do repositório",
                            "Publicar e disponibilizar a aplicação em produção ou teste",
                            "Renomear variáveis locais no editor",
                            "Formatar o disco rígido do servidor"
                        ),
                        "Publicar e disponibilizar a aplicação em produção ou teste",
                        "Publicar e disponibilizar a aplicação em produção ou teste",
                        "Pense no momento em que o sistema sai da máquina local e vai para o servidor."
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Correspondência",
                        "Associe cada termo de programação em inglês ao seu significado em português:",
                        List.of(
                            "Source Code => Código-fonte",
                            "Database => Banco de dados",
                            "Bug => Falha no sistema"
                        ),
                        "Source Code => Código-fonte | Database => Banco de dados | Bug => Falha no sistema",
                        "Source Code => Código-fonte | Database => Banco de dados | Bug => Falha no sistema",
                        "Observe as palavras parecidas com o português como 'Database'."
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Verdadeiro ou Falso",
                        "Verdadeiro ou Falso: A expressão 'open-source software' refere-se a programas cujo código-fonte é aberto para leitura e colaboração.",
                        List.of("Verdadeiro", "Falso"),
                        "Verdadeiro",
                        "Verdadeiro",
                        "'Open' significa aberto e 'source' vem de source code (código-fonte)."
                    )
                )
            ),
            new ExerciseSeedSpec(
                "02. Daily Dev Routine: Present Simple & Continuous",
                "Gramática",
                "Rotina e Tempos Verbais",
                "Fácil",
                "Compare hábitos diários no trabalho/estudos com ações em andamento neste exato momento.",
                null,
                "Use Present Simple para rotinas ('every morning') e Present Continuous para ações acontecendo agora ('right now').",
                List.of(
                    new ExerciseQuestionRequestDTO(
                        "Completar",
                        "Complete a frase com o Present Continuous de 'fix': 'Right now our team ___ (fix) a critical login bug.' (Digite apenas as duas palavras do verbo)",
                        List.of(),
                        "is fixing",
                        "is fixing",
                        "'Our team' equivale a 'it' (singular), pedindo 'is' + verbo com '-ing'."
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Múltipla Escolha",
                        "Escolha a forma correta no Present Simple: 'Every morning, Lucas ___ automated tests before opening a Pull Request.'",
                        List.of("run", "runs", "running", "is run"),
                        "runs",
                        "runs",
                        "Na 3ª pessoa do singular (Lucas = he), acrescenta-se '-s' ao verbo no presente."
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Verdadeiro ou Falso",
                        "Verdadeiro ou Falso: Expressões como 'currently' e 'at the moment' normalmente combinam com o Present Continuous.",
                        List.of("Verdadeiro", "Falso"),
                        "Verdadeiro",
                        "Verdadeiro",
                        "Marcadores de tempo imediato indicam ações em progresso."
                    )
                )
            ),
            new ExerciseSeedSpec(
                "03. Listening & Dictation: Tech Stand-up Meetings",
                "Audição",
                "Compreensão Auditiva",
                "Médio",
                "Treine a escuta ativa com frases reais usadas em reuniões diárias de tecnologia.",
                null,
                "Ouça o áudio no player da questão com atenção à velocidade normal e lenta.",
                List.of(
                    new ExerciseQuestionRequestDTO(
                        "Áudio",
                        "Ouça atentamente a atualização de stand-up e digite exatamente a frase em inglês:",
                        List.of(),
                        null,
                        "Good morning team, yesterday I finished the API tests",
                        "Foque nas palavras-chave: 'morning', 'yesterday' e 'API tests'.",
                        "Good morning team, yesterday I finished the API tests"
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Múltipla Escolha",
                        "Em uma reunião técnica remota, o que significa dizer 'You are on mute'?",
                        List.of(
                            "Sua internet caiu completamente",
                            "Seu microfone está silenciado",
                            "A câmera está em alta resolução",
                            "O compartilhamento de tela travou"
                        ),
                        "Seu microfone está silenciado",
                        "Seu microfone está silenciado",
                        "'Mute' refere-se ao botão de silenciar o áudio."
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Áudio",
                        "Ouça o pedido educado durante a apresentação e transcreva a frase em inglês:",
                        List.of(),
                        null,
                        "Could you share your screen please",
                        "'Share your screen' significa compartilhar sua tela.",
                        "Could you share your screen please"
                    )
                )
            ),
            new ExerciseSeedSpec(
                "04. Simple Past & Incident Debugging",
                "Gramática",
                "Passado e Resolução de Problemas",
                "Médio",
                "Pratique verbos irregulares no passado simples e tradução de relatos de incidentes.",
                null,
                "Muitos verbos importantes em TI ('go', 'write', 'build', 'run') têm passado irregular!",
                List.of(
                    new ExerciseQuestionRequestDTO(
                        "Múltipla Escolha",
                        "Complete no Simple Past: 'Last night the database server ___ down for five minutes during the backup.'",
                        List.of("go", "goes", "went", "gone"),
                        "went",
                        "went",
                        "'Go down' significa sair do ar; no Simple Past o verbo 'go' vira 'went'."
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Correspondência",
                        "Associe cada verbo de engenharia no infinitivo à sua forma no Simple Past:",
                        List.of(
                            "Write => Wrote",
                            "Build => Built",
                            "Run => Ran"
                        ),
                        "Write => Wrote | Build => Built | Run => Ran",
                        "Write => Wrote | Build => Built | Run => Ran",
                        "Atenção às vogais que mudam no passado irregular."
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Tradução",
                        "Traduza para o português: 'Yesterday we solved the problem quickly.'",
                        List.of(),
                        "Ontem nós resolvemos o problema rapidamente",
                        "Ontem nós resolvemos o problema rapidamente",
                        "'Solved' é o passado de 'solve' (resolver) e 'quickly' = rapidamente."
                    )
                )
            ),
            new ExerciseSeedSpec(
                "05. Reading Lab: Cloud Architecture & Cybersecurity",
                "Leitura",
                "Leitura Técnica e Segurança",
                "Médio",
                "Interpretação de texto técnico sobre computação em nuvem, HTTPS e autenticação multifator (MFA).",
                "Text Reading (3 INFO):\n"
                    + "Modern web platforms rely on cloud infrastructure to remain scalable and available worldwide. "
                    + "To protect user data in transit, applications enforce HTTPS encryption across every API endpoint. "
                    + "In addition, engineering teams enable Multi-Factor Authentication (MFA) so a leaked password alone "
                    + "cannot compromise administrative accounts.",
                "Leia o parágrafo base acima antes de responder às três variações de questões.",
                List.of(
                    new ExerciseQuestionRequestDTO(
                        "Verdadeiro ou Falso",
                        "Verdadeiro ou Falso (de acordo com o texto): Se uma plataforma usar apenas senha simples sem MFA, ela garante proteção extra mesmo em caso de vazamento de senha.",
                        List.of("Verdadeiro", "Falso"),
                        "Falso",
                        "Falso",
                        "Releia a última frase do texto: é justamente o MFA que protege quando uma senha vaza."
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Múltipla Escolha",
                        "De acordo com o texto, qual protocolo é utilizado para proteger os dados em trânsito nas requisições da API?",
                        List.of("FTP sem criptografia", "HTTPS", "Telnet", "SMTP aberto"),
                        "HTTPS",
                        "HTTPS",
                        "O texto cita explicitamente 'enforce HTTPS encryption'."
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Discursiva",
                        "Responda em 1 ou 2 frases (em inglês ou português): Por que equipes de desenvolvimento adotam MFA além de HTTPS?",
                        List.of(),
                        null,
                        "Because MFA adds a second verification layer so a leaked password alone cannot compromise user or admin accounts.",
                        "Questão discursiva: será encaminhada para correção do professor!"
                    )
                )
            ),
            new ExerciseSeedSpec(
                "06. Tech Interview & Essential Phrasal Verbs",
                "Conversação",
                "Carreira Internacional",
                "Difícil",
                "Prepare-se para entrevistas técnicas em inglês dominando phrasal verbs e comunicação clara.",
                null,
                "Phrasal verbs combinam um verbo e uma preposição/advérbio criando um significado específico.",
                List.of(
                    new ExerciseQuestionRequestDTO(
                        "Múltipla Escolha",
                        "Em uma entrevista técnica, qual resposta em inglês demonstra melhor postura colaborativa ao lidar com um bug difícil?",
                        List.of(
                            "I never make mistakes so I ignore error logs.",
                            "I analyze the stack trace, reproduce the bug, and collaborate with my team to fix it.",
                            "I delete the failing test so the build turns green.",
                            "I wait for someone else to notice weeks later."
                        ),
                        "I analyze the stack trace, reproduce the bug, and collaborate with my team to fix it.",
                        "I analyze the stack trace, reproduce the bug, and collaborate with my team to fix it.",
                        "Busque a opção que combina análise técnica e trabalho em equipe."
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Áudio",
                        "Ouça a apresentação profissional e digite a frase exata ouvida:",
                        List.of(),
                        null,
                        "I enjoy solving complex backend problems with my team",
                        "Observe 'enjoy + verbo com -ing' ('solving').",
                        "I enjoy solving complex backend problems with my team"
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Correspondência",
                        "Relacione cada phrasal verb muito usado por programadores ao seu significado correspondente:",
                        List.of(
                            "Look into => Investigar em detalhes",
                            "Figure out => Descobrir / Resolver",
                            "Back up => Fazer cópia de segurança"
                        ),
                        "Look into => Investigar em detalhes | Figure out => Descobrir / Resolver | Back up => Fazer cópia de segurança",
                        "Look into => Investigar em detalhes | Figure out => Descobrir / Resolver | Back up => Fazer cópia de segurança",
                        "'Look into' = examinar internamente; 'Back up' = criar backup."
                    )
                )
            ),
            new ExerciseSeedSpec(
                "07. Sprint Planning: Modal Verbs & Future Plans",
                "Escrita",
                "Planejamento e Redação Técnica",
                "Difícil",
                "Aplique verbos modais de recomendação/obrigação e redija planos futuros para uma sprint.",
                null,
                "Verbos modais ('should', 'must', 'might') não recebem '-s' nem 'to' antes do próximo verbo.",
                List.of(
                    new ExerciseQuestionRequestDTO(
                        "Completar",
                        "Complete o conselho técnico usando o modal 'should': 'You ___ back up the database before running migrations.' (Digite apenas uma palavra)",
                        List.of(),
                        "should",
                        "should",
                        "Para dar uma recomendação técnica forte em inglês, usamos 'should' (deveria)."
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Verdadeiro ou Falso",
                        "Verdadeiro ou Falso: Na língua inglesa, o verbo modal 'must' expressa obrigação mais forte do que 'might'.",
                        List.of("Verdadeiro", "Falso"),
                        "Verdadeiro",
                        "Verdadeiro",
                        "'Must' indica necessidade/obrigação; 'might' indica possibilidade."
                    ),
                    new ExerciseQuestionRequestDTO(
                        "Discursiva",
                        "Escreva 1 frase completa em inglês descrevendo uma meta da próxima sprint usando 'going to' ou 'should' (ex: Next week our squad is going to release...).",
                        List.of(),
                        null,
                        "Next week our team is going to deploy the new dashboard and we should run all tests first.",
                        "Questão aberta de produção escrita avaliada pelo professor!"
                    )
                )
            )
        );
    }
}
