package com.duolinfo.ia.proj.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Popula dados de demonstração apenas em ambientes dev/docker com PostgreSQL,
 * sem sobrescrever o super admin nem violar constraints de Flyway V1-V4.
 */
@Component
@Profile({"dev", "docker"})
@Order(100)
public class ComprehensiveDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ComprehensiveDataSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final boolean demoEnabled;

    public ComprehensiveDataSeeder(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.demo-enabled:true}") boolean demoEnabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.demoEnabled = demoEnabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!demoEnabled || !isPostgreSql()) {
            return;
        }
        try {
            seedDemoData();
        } catch (Exception ex) {
            log.warn("ComprehensiveDataSeeder ignorado com segurança devido a estado inconsistente: {}", ex.getMessage());
        }
    }

    private boolean isPostgreSql() {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            String productName = meta.getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("postgresql");
        } catch (Exception ex) {
            return false;
        }
    }

    private void seedDemoData() {
        Long demoCheck = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = 'professor@duolingo.com'",
            Long.class
        );
        if (demoCheck != null && demoCheck > 0) {
            return;
        }

        Long institutionId = findOrCreateInstitution();
        Long schoolId = findOrCreateSchool(institutionId);
        Long class1Id = findOrCreateClass("Turma de Inglês Básico A1", "2026", "NOTURNO", "Iniciante", schoolId, institutionId);
        Long class2Id = findOrCreateClass("Turma de Espanhol Intermediário B1", "2026", "VESPERTINO", "Intermediário", schoolId, institutionId);
        findOrCreateClass("Turma de Francês Conversação A2", "2026", "MATUTINO", "Iniciante-Médio", schoolId, institutionId);

        String defaultHash = passwordEncoder.encode("duadmin");

        Long teacherId = findOrCreateUser("Prof. Carlos Mendes", "professor@duolingo.com", defaultHash, "MASCULINO", 45);
        Long student1Id = findOrCreateUser("Lucas Oliveira", "aluno@duolingo.com", defaultHash, "MASCULINO", 20);
        Long student2Id = findOrCreateUser("Maria Eduarda Santos", "maria@duolingo.com", defaultHash, "FEMININO", 25);

        jdbcTemplate.update("""
            INSERT INTO teachers (id, institution_id, approved, approval_status, institution_admin, primary_institution_admin, bibliography, created_at)
            VALUES (?, ?, true, 'APPROVED', false, false, 'Especialista em Língua Inglesa e Espanhola com foco em conversação ativa.', CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO NOTHING
            """, teacherId, institutionId);

        Long langCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM teacher_taught_languages WHERE teacher_id = ?", Long.class, teacherId
        );
        if (langCount == null || langCount == 0) {
            jdbcTemplate.update("INSERT INTO teacher_taught_languages (teacher_id, language_name) VALUES (?, 'Inglês'), (?, 'Espanhol')", teacherId, teacherId);
        }

        Long specCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM teacher_specialization_areas WHERE teacher_id = ?", Long.class, teacherId
        );
        if (specCount == null || specCount == 0) {
            jdbcTemplate.update("INSERT INTO teacher_specialization_areas (teacher_id, specialization_area) VALUES (?, 'Conversação Prática'), (?, 'Gramática Aplicada')", teacherId, teacherId);
        }

        seedTeacherAssignment(class1Id, teacherId, "Professor titular de Inglês A1");
        seedTeacherAssignment(class2Id, teacherId, "Professor titular de Espanhol B1");

        jdbcTemplate.update("""
            INSERT INTO students (id, institution_id, current_streak, longest_streak, last_active_date, streak_freezes, today_xp_earned, daily_xp_goal)
            VALUES (?, ?, 7, 14, CURRENT_DATE, 2, 45, 50)
            ON CONFLICT (id) DO NOTHING
            """, student1Id, institutionId);

        jdbcTemplate.update("""
            INSERT INTO students (id, institution_id, current_streak, longest_streak, last_active_date, streak_freezes, today_xp_earned, daily_xp_goal)
            VALUES (?, ?, 4, 10, CURRENT_DATE, 1, 30, 30)
            ON CONFLICT (id) DO NOTHING
            """, student2Id, institutionId);

        seedStudentLanguage(student1Id, "Inglês", "A2 - Pré-Intermediário", 340);
        seedStudentLanguage(student1Id, "Espanhol", "A1 - Iniciante", 120);
        seedStudentLanguage(student2Id, "Espanhol", "B1 - Intermediário", 520);
        seedStudentLanguage(student2Id, "Francês", "A1 - Iniciante", 150);

        seedEnrollment(student1Id, class1Id, "Matrícula regular em Inglês A1");
        seedEnrollment(student2Id, class2Id, "Matrícula regular em Espanhol B1");

        String exercise1Questions = """
            [
                {
                    "style": "Objetiva",
                    "question": "How do you say 'Bom dia' in English?",
                    "options": ["Good morning", "Good afternoon", "Good night", "Goodbye"],
                    "correctAnswer": "Good morning",
                    "expectedAnswer": "Good morning"
                },
                {
                    "style": "Objetiva",
                    "question": "Select the correct sentence to introduce yourself:",
                    "options": ["I have 20 years.", "My name is Lucas and I am pleased to meet you.", "Me name Lucas.", "I am name Lucas."],
                    "correctAnswer": "My name is Lucas and I am pleased to meet you.",
                    "expectedAnswer": "My name is Lucas and I am pleased to meet you."
                }
            ]
            """;

        String exercise2Questions = """
            [
                {
                    "style": "Objetiva",
                    "question": "Qual é a tradução de 'desayuno' em espanhol?",
                    "options": ["Almoço", "Café da manhã", "Jantar", "Sobremesa"],
                    "correctAnswer": "Café da manhã",
                    "expectedAnswer": "Café da manhã"
                },
                {
                    "style": "Objetiva",
                    "question": "Complete a frase: '¿Podría traerme la _____ por favor?'",
                    "options": ["cuenta", "cama", "carta de crédito", "coche"],
                    "correctAnswer": "cuenta",
                    "expectedAnswer": "cuenta"
                }
            ]
            """;

        seedExercise("Introdução e Saudações Cotidianas", "Gramática e Vocabulário", "Saudações", "Inglês", "Fácil",
            "Pratique saudações básicas e apresentação pessoal em inglês.",
            "Nesta atividade você aprenderá como cumprimentar pessoas formal e informalmente.",
            exercise1Questions, teacherId, class1Id);

        seedExercise("Español Práctico: En el Restaurante", "Vocabulário e Conversação", "Alimentos", "Espanhol", "Médio",
            "Vocabulário essencial para pedir comida e interagir em restaurantes na Espanha e América Latina.",
            "Aprenda vocabulário útil para pedir pratos e bebidas em espanhol.",
            exercise2Questions, teacherId, class2Id);

        seedSpacedReview(student1Id, "Inglês", "Knowledge", "Conhecimento", "Knowledge is power.", "Conhecimento é poder.", "A1", "vocabulário,básico", 1, 1, 2.5, 1, 10);
        seedSpacedReview(student1Id, "Inglês", "Challenge", "Desafio", "Learning a language is an exciting challenge.", "Aprender um idioma é um desafio empolgante.", "A1", "vocabulário,cotidiano", 2, 3, 2.6, 2, 20);
        seedSpacedReview(student1Id, "Inglês", "Schedule", "Cronograma / Horário", "What is your study schedule today?", "Qual é o seu cronograma de estudo hoje?", "A2", "estudos,rotina", 0, 1, 2.5, 0, 0);

        seedLearningPath(student1Id, "Inglês", "A1", "B1", 7, 20, 35.0, 40.0, 45.0, 30.0, 25.0, 50.0, 35.0);
        seedLearningPath(student2Id, "Espanhol", "B1", "B2", 4, 25, 62.0, 65.0, 70.0, 60.0, 55.0, 75.0, 60.0);

        seedModule("Inglês", "A1", "Gramática", "Vocabulário, Leitura", "Aprenda cumprimentos, alfabeto e apresentação pessoal.", 1, 50, "Fundamentos do Idioma e Saudações");
        seedModule("Inglês", "A1", "Vocabulário", "Vocabulário Cotidiano", "Construa vocabulário com objetos de casa, escola e trabalho.", 2, 100, "Substantivos e Objetos Cotidianos");
        seedModule("Inglês", "A1", "Gramática", "Estruturas Verbais", "Formule frases afirmativas, negativas e perguntas cotidianas.", 3, 150, "Verbos no Presente Simples");

        seedCommunityDemo(student1Id, student2Id);

        log.info("Dados de demonstração populados com sucesso pelo ComprehensiveDataSeeder.");
    }

    private Long findOrCreateInstitution() {
        var existing = jdbcTemplate.queryForList("SELECT id FROM institutions WHERE code = 'CEFET-NOVA-IGUACU' LIMIT 1", Long.class);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return jdbcTemplate.queryForObject("""
            INSERT INTO institutions (name, code, email, phone, website, city, state, country, address, description, active, created_at, updated_at)
            VALUES ('CEFET/RJ - Campus Nova Iguaçu', 'CEFET-NOVA-IGUACU', 'cefet.novaiguacu@duolinfo.local', '(21) 3759-9800', 'https://www.cefet-rj.br', 'Nova Iguaçu', 'RJ', 'Brasil', 'Estrada de Adrianópolis, 1317', 'Campus Nova Iguaçu do CEFET/RJ', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """, Long.class);
    }

    private Long findOrCreateSchool(Long institutionId) {
        var existing = jdbcTemplate.queryForList("SELECT id FROM schools WHERE institution_id = ? LIMIT 1", Long.class, institutionId);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return jdbcTemplate.queryForObject("""
            INSERT INTO schools (name, inep_code, institution_id, created_at, updated_at)
            VALUES ('Departamento de Línguas Estrangeiras e Letras', '33001234', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """, Long.class, institutionId);
    }

    private Long findOrCreateClass(String name, String year, String shift, String gradeLevel, Long schoolId, Long institutionId) {
        var existing = jdbcTemplate.queryForList(
            "SELECT id FROM classes WHERE name = ? AND institution_id = ? LIMIT 1",
            Long.class, name, institutionId
        );
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return jdbcTemplate.queryForObject("""
            INSERT INTO classes (name, school_year, shift, grade_level, school_id, institution_id, active, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """, Long.class, name, year, shift, gradeLevel, schoolId, institutionId);
    }

    private Long findOrCreateUser(String name, String email, String passwordHash, String gender, int dailyGoalMinutes) {
        var existing = jdbcTemplate.queryForList("SELECT id FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1", Long.class, email);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return jdbcTemplate.queryForObject("""
            INSERT INTO users (name, email, password_hash, is_admin, active, gender, ui_language, theme_mode, daily_goal_minutes, version)
            VALUES (?, ?, ?, false, true, ?, 'pt', 'light', ?, 0)
            RETURNING id
            """, Long.class, name, email, passwordHash, gender, dailyGoalMinutes);
    }

    private void seedTeacherAssignment(Long classId, Long teacherId, String reason) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM class_teacher_assignments WHERE class_id = ? AND teacher_id = ?",
            Long.class, classId, teacherId
        );
        if (count == null || count == 0) {
            jdbcTemplate.update("""
                INSERT INTO class_teacher_assignments (class_id, teacher_id, start_date, reason)
                VALUES (?, ?, CURRENT_DATE, ?)
                """, classId, teacherId, reason);
        }
    }

    private void seedStudentLanguage(Long studentId, String languageName, String level, int score) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM student_languages WHERE student_id = ? AND language_name = ?",
            Long.class, studentId, languageName
        );
        if (count == null || count == 0) {
            jdbcTemplate.update(
                "INSERT INTO student_languages (student_id, language_name, level, score) VALUES (?, ?, ?, ?)",
                studentId, languageName, level, score
            );
        }
    }

    private void seedEnrollment(Long studentId, Long classId, String notes) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM enrollments WHERE student_id = ? AND class_id = ?",
            Long.class, studentId, classId
        );
        if (count == null || count == 0) {
            jdbcTemplate.update("""
                INSERT INTO enrollments (student_id, class_id, enrollment_date, status, notes)
                VALUES (?, ?, CURRENT_DATE, 'ACTIVE', ?)
                """, studentId, classId, notes);
        }
    }

    private void seedExercise(String title, String category, String theme, String language, String level,
                              String description, String contentBody, String questionsJson, Long teacherId, Long classId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM learning_exercises WHERE title = ? AND teacher_id = ?",
            Long.class, title, teacherId
        );
        if (count == null || count == 0) {
            jdbcTemplate.update("""
                INSERT INTO learning_exercises (title, category, theme, language_name, proficiency_level, description, content_body, due_date, questions_json, active, created_at, teacher_id, school_class_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_DATE + 7, ?, true, CURRENT_TIMESTAMP, ?, ?)
                """, title, category, theme, language, level, description, contentBody, questionsJson, teacherId, classId);
        }
    }

    private void seedSpacedReview(Long studentId, String language, String term, String translation, String exSentence,
                                  String exTranslation, String level, String tags, int rep, int interval, double ease,
                                  int totalReviews, int xpEarned) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM spaced_review_items WHERE student_id = ? AND term = ?",
            Long.class, studentId, term
        );
        if (count == null || count == 0) {
            jdbcTemplate.update("""
                INSERT INTO spaced_review_items (student_id, language_name, term, translation, example_sentence, example_translation, proficiency_level, tags, repetition_number, interval_days, ease_factor, due_date, created_at, total_reviews, lapse_count, xp_earned)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE, CURRENT_TIMESTAMP, ?, 0, ?)
                """, studentId, language, term, translation, exSentence, exTranslation, level, tags, rep, interval, ease, totalReviews, xpEarned);
        }
    }

    private void seedLearningPath(Long studentId, String language, String currentCefr, String targetCefr, int streak,
                                  int dailyMinutes, double progress, double grammar, double vocab, double listening,
                                  double speaking, double reading, double writing) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM student_learning_paths WHERE student_id = ? AND language = ?",
            Long.class, studentId, language
        );
        if (count == null || count == 0) {
            jdbcTemplate.update("""
                INSERT INTO student_learning_paths (student_id, language, current_cefr_level, target_cefr_level, current_streak_days, daily_goal_minutes, overall_progress_percent, grammar_competence, vocabulary_competence, listening_competence, speaking_competence, reading_competence, writing_competence, last_diagnostic_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, studentId, language, currentCefr, targetCefr, streak, dailyMinutes, progress, grammar, vocab, listening, speaking, reading, writing);
        }
    }

    private void seedModule(String language, String cefr, String category, String competencies,
                            String description, int order, int requiredXp, String title) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM learning_path_modules WHERE language = ? AND module_order = ?",
            Long.class, language, order
        );
        if (count == null || count == 0) {
            jdbcTemplate.update("""
                INSERT INTO learning_path_modules (language, cefr_level, category, competencies_focused, description, module_order, required_xp, title)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, language, cefr, category, competencies, description, order, requiredXp, title);
        }
    }

    private void seedCommunityDemo(Long authorStudentId, Long reviewerStudentId) {
        Long subCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM community_submissions", Long.class);
        if (subCount != null && subCount > 0) {
            return;
        }
        Long subId = jdbcTemplate.queryForObject("""
            INSERT INTO community_submissions (author_student_id, author_name, language, target_level, prompt_title, content, status, review_count, is_public, created_at)
            VALUES (?, 'Lucas Oliveira', 'Inglês', 'A1', 'Descreva o seu dia a dia e seus hobbies em um parágrafo.', 'Hello! My name is Lucas. Every morning I wake up at 7am and drink coffee. I like reading books and practice English with Duolinfo.', 'REVIEWED', 1, true, CURRENT_TIMESTAMP)
            RETURNING id
            """, Long.class, authorStudentId);

        if (subId != null) {
            jdbcTemplate.update("""
                INSERT INTO community_reviews (submission_id, reviewer_student_id, reviewer_name, reviewer_fluency_level, overall_feedback, corrected_content, native_speaker_tips, rating, created_at)
                VALUES (?, ?, 'Maria Eduarda Santos', 'B1 - Intermediário', 'Excelente texto inicial, Lucas! Muito claro. Uma pequena dica: em vez de "and practice", use "and practicing" após "I like". Continue assim!', 'Hello! My name is Lucas. Every morning I wake up at 7am and drink coffee. I like reading books and practicing English with Duolinfo.', 'Lembre-se de usar gerúndio (-ing) após verbos de preferência como "like" ao listar múltiplos hobbies.', 5, CURRENT_TIMESTAMP)
                """, subId, reviewerStudentId);
        }
    }
}
