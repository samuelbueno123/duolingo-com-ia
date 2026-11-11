-- Flyway V2: Performance Indexes, Integrity Constraints and Concurrency Protections

-- 1. Ensure version column exists for Optimistic Locking
ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- 2. Unique Constraints for Business Integrity
CREATE UNIQUE INDEX IF NOT EXISTS uk_student_language_name
    ON student_languages (student_id, language_name);

CREATE UNIQUE INDEX IF NOT EXISTS uk_student_vocab_term
    ON student_vocabularies (student_id, language, term);

CREATE UNIQUE INDEX IF NOT EXISTS uk_class_school_year_name_shift
    ON classes (school_id, school_year, name, shift);

CREATE UNIQUE INDEX IF NOT EXISTS uk_student_achievement
    ON student_achievements (student_id, achievement_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_catalog_option
    ON catalog_options (catalog_type, option_value);

-- 3. Partial Unique Indexes to prevent Concurrency Race Conditions
-- Prevents a student from being actively enrolled twice in the same class
CREATE UNIQUE INDEX IF NOT EXISTS uk_active_enrollment
    ON enrollments (student_id, class_id)
    WHERE status = 'ACTIVE';

-- Prevents assigning the same active teacher twice to the same class
CREATE UNIQUE INDEX IF NOT EXISTS uk_active_class_teacher
    ON class_teacher_assignments (class_id, teacher_id)
    WHERE end_date IS NULL;

-- 4. High-Impact Performance Indexes
-- Users
CREATE INDEX IF NOT EXISTS idx_users_name ON users(name);
CREATE INDEX IF NOT EXISTS idx_users_is_admin ON users(is_admin);

-- Teachers
CREATE INDEX IF NOT EXISTS idx_instituicao_professor ON teachers(institution_id, approved);
CREATE INDEX IF NOT EXISTS idx_instituicao_administradores ON teachers(institution_id, institution_admin, primary_institution_admin);
CREATE INDEX IF NOT EXISTS idx_teacher_taught_lang_teacher ON teacher_taught_languages(teacher_id);
CREATE INDEX IF NOT EXISTS idx_teacher_spec_teacher ON teacher_specialization_areas(teacher_id);
CREATE INDEX IF NOT EXISTS idx_teacher_documents_teacher ON teacher_documents(teacher_id);

-- Students
CREATE INDEX IF NOT EXISTS idx_instituicao_aluno ON students(institution_id);
CREATE INDEX IF NOT EXISTS idx_student_languages_student ON student_languages(student_id);
CREATE INDEX IF NOT EXISTS idx_student_languages_lang ON student_languages(language_name);

-- Schools & Classes
CREATE INDEX IF NOT EXISTS idx_school_institution ON schools(institution_id);
CREATE INDEX IF NOT EXISTS idx_instituicao_turma ON classes(institution_id, school_id);

-- Class Teacher Assignments
CREATE INDEX IF NOT EXISTS idx_cta_class ON class_teacher_assignments(class_id);
CREATE INDEX IF NOT EXISTS idx_cta_teacher ON class_teacher_assignments(teacher_id);
CREATE INDEX IF NOT EXISTS idx_cta_class_end ON class_teacher_assignments(class_id, end_date);

-- Enrollments
CREATE INDEX IF NOT EXISTS idx_enrollments_student ON enrollments(student_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_class ON enrollments(class_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_class_status ON enrollments(class_id, status);
CREATE INDEX IF NOT EXISTS idx_enrollments_student_status ON enrollments(student_id, status);

-- Learning Exercises & Submissions
CREATE INDEX IF NOT EXISTS idx_exercises_class_active ON learning_exercises(school_class_id, active, created_at);
CREATE INDEX IF NOT EXISTS idx_exercises_teacher_created ON learning_exercises(teacher_id, created_at);
CREATE INDEX IF NOT EXISTS idx_submissions_student_date ON exercise_submissions(student_id, submitted_at);
CREATE INDEX IF NOT EXISTS idx_submissions_exercise_date ON exercise_submissions(exercise_id, submitted_at);
CREATE INDEX IF NOT EXISTS idx_submissions_status ON exercise_submissions(review_status);

-- Notifications
CREATE INDEX IF NOT EXISTS idx_admin_notif_target ON admin_notifications(institution_id, is_read);
CREATE INDEX IF NOT EXISTS idx_student_notif_student ON student_notifications(student_id);
CREATE INDEX IF NOT EXISTS idx_student_notif_read ON student_notifications(student_id, read);

-- Spaced Repetition & Mistakes & Vocab
CREATE INDEX IF NOT EXISTS idx_srs_student_due ON spaced_review_items(student_id, due_date);
CREATE INDEX IF NOT EXISTS idx_srs_student_lang ON spaced_review_items(student_id, language_name);
CREATE INDEX IF NOT EXISTS idx_student_mistakes_student ON student_mistakes(student_id);
CREATE INDEX IF NOT EXISTS idx_student_mistakes_resolved ON student_mistakes(student_id, resolved);
CREATE INDEX IF NOT EXISTS idx_vocab_student ON student_vocabularies(student_id);
CREATE INDEX IF NOT EXISTS idx_vocab_student_lang ON student_vocabularies(student_id, language);

-- Announcements & Notes
CREATE INDEX IF NOT EXISTS idx_announcements_class ON class_announcements(school_class_id);
CREATE INDEX IF NOT EXISTS idx_announcements_teacher ON class_announcements(teacher_id);
CREATE INDEX IF NOT EXISTS idx_notes_teacher ON teacher_student_notes(teacher_id);
CREATE INDEX IF NOT EXISTS idx_notes_student ON teacher_student_notes(student_id);

-- AI & Community
CREATE INDEX IF NOT EXISTS idx_ai_session_student ON ai_conversation_sessions(student_id);
CREATE INDEX IF NOT EXISTS idx_ai_message_session ON ai_conversation_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_community_sub_student ON community_submissions(author_student_id);
CREATE INDEX IF NOT EXISTS idx_community_rev_sub ON community_reviews(submission_id);
