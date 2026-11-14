-- Comprehensive Demo Seed Script for Duolinfo

-- 1. Ensure Institutions
INSERT INTO institutions (id, name, code, email, phone, website, city, state, country, address, description, active, created_at, updated_at)
VALUES (1, 'CEFET/RJ - Campus Nova Iguaçu', 'CEFET-NOVA-IGUACU', 'cefet.novaiguacu@duolinfo.local', '(21) 3759-9800', 'https://www.cefet-rj.br', 'Nova Iguaçu', 'RJ', 'Brasil', 'Estrada de Adrianópolis, 1317', 'Campus Nova Iguaçu do CEFET/RJ', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET active = true, updated_at = CURRENT_TIMESTAMP;

-- 2. Ensure Schools
INSERT INTO schools (id, name, inep_code, institution_id, created_at, updated_at)
VALUES (1, 'Departamento de Línguas Estrangeiras e Letras', '33001234', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = CURRENT_TIMESTAMP;

-- 3. Ensure Classes
INSERT INTO classes (id, name, school_year, shift, grade_level, language_name, school_id, institution_id, active, created_at, updated_at)
VALUES 
    (1, 'Turma de Inglês Básico A1', '2026', 'NOTURNO', 'Iniciante', 'Inglês', 1, 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Turma de Espanhol Intermediário B1', '2026', 'VESPERTINO', 'Intermediário', 'Espanhol', 1, 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Turma de Francês Conversação A2', '2026', 'MATUTINO', 'Iniciante-Médio', 'Francês', 1, 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET language_name = EXCLUDED.language_name, active = true, updated_at = CURRENT_TIMESTAMP;

-- 4. Ensure Users (Password hash for 'duadmin')
-- Bcrypt hash for 'duadmin': $2a$10$o8.jQJbU1e82EszV27H30O2LdYF3q6R8J4u4aFhP27j1VlhgNfehK
INSERT INTO users (id, name, email, password_hash, is_admin, active, gender, ui_language, theme_mode, daily_goal_minutes, version)
VALUES 
    (1, 'Administrador Duolinfo', 'admin@duolingo.com', '$2a$10$o8.jQJbU1e82EszV27H30O2LdYF3q6R8J4u4aFhP27j1VlhgNfehK', true, true, 'MASCULINO', 'pt-BR', 'light', 30, 0),
    (2, 'Prof. Carlos Mendes', 'professor@duolingo.com', '$2a$10$o8.jQJbU1e82EszV27H30O2LdYF3q6R8J4u4aFhP27j1VlhgNfehK', false, true, 'MASCULINO', 'pt-BR', 'light', 45, 0),
    (3, 'Lucas Oliveira', 'aluno@duolingo.com', '$2a$10$o8.jQJbU1e82EszV27H30O2LdYF3q6R8J4u4aFhP27j1VlhgNfehK', false, true, 'MASCULINO', 'pt-BR', 'light', 20, 0),
    (4, 'Maria Eduarda Santos', 'maria@duolingo.com', '$2a$10$o8.jQJbU1e82EszV27H30O2LdYF3q6R8J4u4aFhP27j1VlhgNfehK', false, true, 'FEMININO', 'pt-BR', 'light', 25, 0)
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name,
    active = true,
    password_hash = COALESCE(users.password_hash, EXCLUDED.password_hash);

-- 5. Ensure Teachers
INSERT INTO teachers (id, institution_id, approved, approval_status, institution_admin, primary_institution_admin, bibliography, created_at)
VALUES 
    (2, 1, true, 'APPROVED', false, false, 'Especialista em Língua Inglesa e Espanhola com foco em conversação ativa.', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET 
    approved = true,
    approval_status = 'APPROVED',
    institution_id = 1;

DELETE FROM teacher_taught_languages WHERE teacher_id = 2;
INSERT INTO teacher_taught_languages (teacher_id, language_name) VALUES (2, 'Inglês'), (2, 'Espanhol');

DELETE FROM teacher_specialization_areas WHERE teacher_id = 2;
INSERT INTO teacher_specialization_areas (teacher_id, specialization_area) VALUES (2, 'Conversação Prática'), (2, 'Gramática Aplicada');

INSERT INTO class_teacher_assignments (id, class_id, teacher_id, start_date, reason)
VALUES 
    (1, 1, 2, '2026-02-01', 'Professor titular de Inglês A1'),
    (2, 2, 2, '2026-02-01', 'Professor titular de Espanhol B1')
ON CONFLICT (id) DO NOTHING;

-- 6. Ensure Students
INSERT INTO students (id, institution_id, current_streak, longest_streak, last_active_date, streak_freezes, today_xp_earned, daily_xp_goal)
VALUES 
    (3, 1, 7, 14, CURRENT_DATE, 2, 45, 50),
    (4, 1, 4, 10, CURRENT_DATE, 1, 30, 30)
ON CONFLICT (id) DO UPDATE SET 
    institution_id = 1,
    current_streak = EXCLUDED.current_streak;

DELETE FROM student_languages WHERE student_id IN (3, 4);
INSERT INTO student_languages (student_id, language_name, level, score)
VALUES 
    (3, 'Inglês', 'A1 - Iniciante', 380),
    (3, 'Espanhol', 'Iniciante', 120),
    (4, 'Espanhol', 'B1 - Intermediário', 520),
    (4, 'Francês', 'A1 - Iniciante', 150);

INSERT INTO enrollments (id, student_id, class_id, enrollment_date, status, notes)
VALUES 
    (1, 3, 1, '2026-02-10', 'ACTIVE', 'Matrícula regular em Inglês A1'),
    (2, 4, 2, '2026-02-10', 'ACTIVE', 'Matrícula regular em Espanhol B1')
ON CONFLICT (id) DO NOTHING;

-- 7. Ensure Learning Exercises
INSERT INTO learning_exercises (id, title, category, theme, language_name, proficiency_level, description, content_body, due_date, questions_json, active, created_at, teacher_id, school_class_id)
VALUES 
    (1, 'Introdução e Saudações Cotidianas', 'Gramática e Vocabulário', 'Saudações', 'Inglês', 'Fácil', 'Pratique saudações básicas e apresentação pessoal em inglês.', 'Nesta atividade você aprenderá como cumprimentar pessoas formal e informalmente.', CURRENT_DATE + 7, '[{"style":"Objetiva","question":"How do you say ''Bom dia'' in English?","options":["Good morning","Good afternoon","Good night","Goodbye"],"correctAnswer":"Good morning","expectedAnswer":"Good morning"},{"style":"Objetiva","question":"Select the correct sentence to introduce yourself:","options":["I have 20 years.","My name is Lucas and I am pleased to meet you.","Me name Lucas.","I am name Lucas."],"correctAnswer":"My name is Lucas and I am pleased to meet you.","expectedAnswer":"My name is Lucas and I am pleased to meet you."}]', true, CURRENT_TIMESTAMP, 2, 1),
    (2, 'Español Práctico: En el Restaurante', 'Vocabulário e Conversação', 'Alimentos', 'Espanhol', 'Médio', 'Vocabulário essencial para pedir comida e interagir em restaurantes na Espanha e América Latina.', 'Aprenda vocabulário útil para pedir pratos e bebidas em espanhol.', CURRENT_DATE + 10, '[{"style":"Objetiva","question":"Qual é a tradução de ''desayuno'' em espanhol?","options":["Almoço","Café da manhã","Jantar","Sobremesa"],"correctAnswer":"Café da manhã","expectedAnswer":"Café da manhã"},{"style":"Objetiva","question":"Complete a frase: ''¿Podría traerme la _____ por favor?''","options":["cuenta","cama","carta de crédito","coche"],"correctAnswer":"cuenta","expectedAnswer":"cuenta"}]', true, CURRENT_TIMESTAMP, 2, 2)
ON CONFLICT (id) DO NOTHING;

-- 8. Spaced Review Items (SRS)
INSERT INTO spaced_review_items (id, student_id, language_name, term, translation, example_sentence, example_translation, proficiency_level, tags, repetition_number, interval_days, ease_factor, due_date, last_reviewed_at, total_reviews, lapse_count, xp_earned)
VALUES 
    (1, 3, 'Inglês', 'Knowledge', 'Conhecimento', 'Knowledge is power.', 'Conhecimento é poder.', 'A1', 'vocabulário,básico', 1, 1, 2.5, CURRENT_DATE, CURRENT_TIMESTAMP, 1, 0, 10),
    (2, 3, 'Inglês', 'Challenge', 'Desafio', 'Learning a language is an exciting challenge.', 'Aprender um idioma é um desafio empolgante.', 'A1', 'vocabulário,cotidiano', 2, 3, 2.6, CURRENT_DATE, CURRENT_TIMESTAMP, 2, 0, 20),
    (3, 3, 'Inglês', 'Schedule', 'Cronograma / Horário', 'What is your study schedule today?', 'Qual é o seu cronograma de estudo hoje?', 'A2', 'estudos,rotina', 0, 1, 2.5, CURRENT_DATE, CURRENT_TIMESTAMP, 0, 0, 0),
    (4, 3, 'Inglês', 'Fluency', 'Fluência', 'Daily practice leads to fluency.', 'A prática diária leva à fluência.', 'B1', 'metas,inspiração', 3, 6, 2.7, CURRENT_DATE + 2, CURRENT_TIMESTAMP, 3, 0, 30),
    (5, 3, 'Inglês', 'Achievement', 'Conquista / Realização', 'Graduation is a great achievement.', 'A formatura é uma grande conquista.', 'B1', 'geral', 1, 2, 2.5, CURRENT_DATE + 1, CURRENT_TIMESTAMP, 1, 0, 10)
ON CONFLICT (id) DO NOTHING;

-- 9. Learning Paths
INSERT INTO student_learning_paths (id, student_id, language, current_cefr_level, target_cefr_level, current_streak_days, daily_goal_minutes, overall_progress_percent, grammar_competence, vocabulary_competence, listening_competence, speaking_competence, reading_competence, writing_competence, last_diagnostic_at, updated_at)
VALUES 
    (1, 3, 'Inglês', 'A1', 'B1', 7, 20, 35.0, 40.0, 45.0, 30.0, 25.0, 50.0, 35.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 4, 'Espanhol', 'B1', 'B2', 4, 25, 62.0, 65.0, 70.0, 60.0, 55.0, 75.0, 60.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO learning_path_modules (id, language, cefr_level, category, competencies_focused, description, module_order, required_xp, title)
VALUES 
    (1, 'Inglês', 'A1', 'Gramática', 'Vocabulário, Leitura', 'Aprenda cumprimentos, alfabeto e apresentação pessoal.', 1, 50, 'Fundamentos do Idioma e Saudações'),
    (2, 'Inglês', 'A1', 'Vocabulário', 'Vocabulário Cotidiano', 'Construa vocabulário com objetos de casa, escola e trabalho.', 2, 100, 'Substantivos e Objetos Cotidianos'),
    (3, 'Inglês', 'A1', 'Gramática', 'Estruturas Verbais', 'Formule frases afirmativas, negativas e perguntas cotidianas.', 3, 150, 'Verbos no Presente Simples'),
    (4, 'Inglês', 'A2', 'Narrativa', 'Compreensão Oral e Escrita', 'Aprenda a relatar eventos que aconteceram ontem ou na infância.', 4, 200, 'Passado Simples e Histórias')
ON CONFLICT (id) DO NOTHING;

-- 10. Community Submissions & Reviews
INSERT INTO community_submissions (id, author_student_id, author_name, language, target_level, prompt_title, content, status, review_count, is_public, created_at)
VALUES 
    (1, 3, 'Lucas Oliveira', 'Inglês', 'A1', 'Descreva o seu dia a dia e seus hobbies em um parágrafo.', 'Hello! My name is Lucas. Every morning I wake up at 7am and drink coffee. I like reading books and practice English with Duolinfo.', 'REVIEWED', 1, true, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO community_reviews (id, submission_id, reviewer_student_id, reviewer_name, reviewer_fluency_level, overall_feedback, corrected_content, native_speaker_tips, rating, created_at)
VALUES 
    (1, 1, 4, 'Maria Eduarda Santos', 'B1 - Intermediário', 'Excelente texto inicial, Lucas! Muito claro. Uma pequena dica: em vez de "and practice", use "and practicing" após "I like". Continue assim!', 'Hello! My name is Lucas. Every morning I wake up at 7am and drink coffee. I like reading books and practicing English with Duolinfo.', 'Lembre-se de usar gerúndio (-ing) após verbos de preferência como "like" ao listar múltiplos hobbies.', 5, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Fix sequence counters
SELECT setval('institutions_id_seq', (SELECT COALESCE(MAX(id), 1) FROM institutions));
SELECT setval('schools_id_seq', (SELECT COALESCE(MAX(id), 1) FROM schools));
SELECT setval('classes_id_seq', (SELECT COALESCE(MAX(id), 3) FROM classes));
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 4) FROM users));
SELECT setval('class_teacher_assignments_id_seq', (SELECT COALESCE(MAX(id), 2) FROM class_teacher_assignments));
SELECT setval('enrollments_id_seq', (SELECT COALESCE(MAX(id), 2) FROM enrollments));
SELECT setval('learning_exercises_id_seq', (SELECT COALESCE(MAX(id), 2) FROM learning_exercises));
SELECT setval('spaced_review_items_id_seq', (SELECT COALESCE(MAX(id), 5) FROM spaced_review_items));
SELECT setval('student_learning_paths_id_seq', (SELECT COALESCE(MAX(id), 2) FROM student_learning_paths));
SELECT setval('learning_path_modules_id_seq', (SELECT COALESCE(MAX(id), 4) FROM learning_path_modules));
SELECT setval('community_submissions_id_seq', (SELECT COALESCE(MAX(id), 1) FROM community_submissions));
SELECT setval('community_reviews_id_seq', (SELECT COALESCE(MAX(id), 1) FROM community_reviews));
