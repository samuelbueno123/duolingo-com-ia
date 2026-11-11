-- Flyway V1: Initial Schema for Duolinfo
-- Creates all base tables with Primary Keys, Foreign Keys and baseline structure

-- 1. Users (Base entity for inheritance)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    google_id VARCHAR(255) UNIQUE,
    google_email_verified BOOLEAN,
    profile_picture TEXT,
    gender VARCHAR(30),
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0
);

-- 2. Institutions
CREATE TABLE IF NOT EXISTS institutions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(255) UNIQUE,
    website VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    country VARCHAR(255),
    address VARCHAR(255),
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 3. Institution Requests
CREATE TABLE IF NOT EXISTS institution_requests (
    id BIGSERIAL PRIMARY KEY,
    requested_name VARCHAR(150) NOT NULL,
    requester_name VARCHAR(150) NOT NULL,
    requester_email VARCHAR(254) NOT NULL,
    city VARCHAR(100),
    state VARCHAR(2),
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

-- 4. Teachers (JOINED inheritance with users)
CREATE TABLE IF NOT EXISTS teachers (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    institution_id BIGINT NOT NULL REFERENCES institutions(id),
    approved BOOLEAN DEFAULT FALSE,
    approval_status VARCHAR(20) DEFAULT 'PENDING',
    institution_admin BOOLEAN NOT NULL DEFAULT FALSE,
    primary_institution_admin BOOLEAN NOT NULL DEFAULT FALSE,
    bibliography TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Teacher ElementCollections
CREATE TABLE IF NOT EXISTS teacher_taught_languages (
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    language_name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS teacher_specialization_areas (
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    specialization_area VARCHAR(255) NOT NULL
);

-- 6. Teacher Documents (BLOBs)
CREATE TABLE IF NOT EXISTS teacher_documents (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA NOT NULL
);

-- 7. Students (JOINED inheritance with users)
CREATE TABLE IF NOT EXISTS students (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    institution_id BIGINT REFERENCES institutions(id),
    current_streak INTEGER DEFAULT 0,
    longest_streak INTEGER DEFAULT 0,
    last_active_date DATE,
    streak_freezes INTEGER DEFAULT 2,
    today_xp_earned INTEGER DEFAULT 0,
    daily_xp_goal INTEGER DEFAULT 50
);

-- 8. Student Languages
CREATE TABLE IF NOT EXISTS student_languages (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    language_name VARCHAR(255) NOT NULL,
    level VARCHAR(255) NOT NULL DEFAULT '1',
    score INTEGER NOT NULL DEFAULT 0
);

-- 9. Schools
CREATE TABLE IF NOT EXISTS schools (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    inep_code VARCHAR(255) UNIQUE,
    institution_id BIGINT NOT NULL REFERENCES institutions(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 10. School Classes (table name: classes)
CREATE TABLE IF NOT EXISTS classes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    school_year VARCHAR(255),
    shift VARCHAR(255),
    grade_level VARCHAR(255),
    school_id BIGINT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    institution_id BIGINT REFERENCES institutions(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 11. Class Teacher Assignments
CREATE TABLE IF NOT EXISTS class_teacher_assignments (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE,
    reason TEXT
);

-- 12. Enrollments
CREATE TABLE IF NOT EXISTS enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    enrollment_date DATE NOT NULL,
    cancellation_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT
);

-- 13. Learning Exercises
CREATE TABLE IF NOT EXISTS learning_exercises (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    category VARCHAR(120),
    theme VARCHAR(140),
    language_name VARCHAR(100) NOT NULL,
    proficiency_level VARCHAR(60) NOT NULL,
    description VARCHAR(3000),
    content_body TEXT,
    media_url VARCHAR(2048),
    due_date DATE,
    questions_json TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    school_class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE
);

-- 14. Exercise Submissions
CREATE TABLE IF NOT EXISTS exercise_submissions (
    id BIGSERIAL PRIMARY KEY,
    exercise_id BIGINT NOT NULL REFERENCES learning_exercises(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    answers_json TEXT NOT NULL,
    correct_count INTEGER NOT NULL DEFAULT 0,
    question_count INTEGER NOT NULL DEFAULT 0,
    xp_earned INTEGER NOT NULL DEFAULT 0,
    feedback TEXT,
    review_status VARCHAR(30) DEFAULT 'AUTO_GRADED',
    question_feedback_json TEXT,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 15. Catalog Options
CREATE TABLE IF NOT EXISTS catalog_options (
    id BIGSERIAL PRIMARY KEY,
    catalog_type VARCHAR(40) NOT NULL,
    option_value VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 16. Password Reset Tokens
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_ip VARCHAR(64),
    attempt_count INTEGER NOT NULL DEFAULT 0
);

-- 17. Admin Notifications
CREATE TABLE IF NOT EXISTS admin_notifications (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    institution_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 18. Student Notifications
CREATE TABLE IF NOT EXISTS student_notifications (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    read BOOLEAN NOT NULL DEFAULT FALSE,
    action_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 19. Achievements & Student Achievements
CREATE TABLE IF NOT EXISTS achievements (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(500) NOT NULL,
    category VARCHAR(80) NOT NULL,
    icon_name VARCHAR(80) NOT NULL DEFAULT 'emoji_events',
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS student_achievements (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    achievement_id BIGINT NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
    earned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by_admin BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_by_admin BOOLEAN NOT NULL DEFAULT FALSE
);

-- 20. Student Learning Paths & Modules
CREATE TABLE IF NOT EXISTS student_learning_paths (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    language VARCHAR(64) NOT NULL,
    current_cefr_level VARCHAR(16) NOT NULL DEFAULT 'A1',
    target_cefr_level VARCHAR(16) NOT NULL DEFAULT 'B2',
    daily_goal_minutes INTEGER NOT NULL DEFAULT 15,
    current_streak_days INTEGER NOT NULL DEFAULT 1,
    overall_progress_percent DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    grammar_competence DOUBLE PRECISION NOT NULL DEFAULT 20.0,
    vocabulary_competence DOUBLE PRECISION NOT NULL DEFAULT 25.0,
    listening_competence DOUBLE PRECISION NOT NULL DEFAULT 15.0,
    speaking_competence DOUBLE PRECISION NOT NULL DEFAULT 15.0,
    reading_competence DOUBLE PRECISION NOT NULL DEFAULT 30.0,
    writing_competence DOUBLE PRECISION NOT NULL DEFAULT 20.0,
    last_diagnostic_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS learning_path_modules (
    id BIGSERIAL PRIMARY KEY,
    language VARCHAR(64) NOT NULL,
    cefr_level VARCHAR(16) NOT NULL,
    module_order INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(64) NOT NULL,
    competencies_focused VARCHAR(128),
    required_xp INTEGER NOT NULL DEFAULT 50
);

CREATE TABLE IF NOT EXISTS student_module_progress (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    module_id BIGINT NOT NULL REFERENCES learning_path_modules(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'LOCKED',
    score_percent DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    completed_at TIMESTAMP
);

-- 21. Student Vocabularies
CREATE TABLE IF NOT EXISTS student_vocabularies (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    language VARCHAR(60) NOT NULL,
    term VARCHAR(120) NOT NULL,
    translation VARCHAR(200) NOT NULL,
    part_of_speech VARCHAR(30),
    example_sentence TEXT,
    mastery_level VARCHAR(30) DEFAULT 'LEARNING',
    times_practiced INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 22. Spaced Review Items
CREATE TABLE IF NOT EXISTS spaced_review_items (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    language_name VARCHAR(60) NOT NULL,
    term VARCHAR(250) NOT NULL,
    translation VARCHAR(250) NOT NULL,
    example_sentence TEXT,
    example_translation TEXT,
    proficiency_level VARCHAR(20) DEFAULT 'A1',
    tags VARCHAR(150),
    repetition_number INTEGER NOT NULL DEFAULT 0,
    interval_days INTEGER NOT NULL DEFAULT 1,
    ease_factor DOUBLE PRECISION NOT NULL DEFAULT 2.5,
    due_date DATE NOT NULL DEFAULT CURRENT_DATE,
    last_reviewed_at TIMESTAMP WITH TIME ZONE,
    total_reviews INTEGER NOT NULL DEFAULT 0,
    lapse_count INTEGER NOT NULL DEFAULT 0,
    xp_earned INTEGER NOT NULL DEFAULT 0
);

-- 23. Student Mistakes
CREATE TABLE IF NOT EXISTS student_mistakes (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    exercise_id BIGINT,
    question TEXT NOT NULL,
    user_wrong_answer TEXT,
    correct_answer TEXT NOT NULL,
    explanation TEXT,
    times_reviewed INTEGER NOT NULL DEFAULT 0,
    consecutive_correct INTEGER NOT NULL DEFAULT 0,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    last_reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 24. AI Conversation Sessions & Messages
CREATE TABLE IF NOT EXISTS ai_conversation_sessions (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    scenario_code VARCHAR(64) NOT NULL,
    scenario_title VARCHAR(255) NOT NULL,
    target_language VARCHAR(64) NOT NULL,
    cefr_level VARCHAR(16) NOT NULL,
    persona_role VARCHAR(128),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    total_turns INTEGER NOT NULL DEFAULT 0,
    grammar_corrections_count INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_conversation_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES ai_conversation_sessions(id) ON DELETE CASCADE,
    sender VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    audio_url VARCHAR(256),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    has_grammar_issues BOOLEAN DEFAULT FALSE,
    grammar_correction TEXT,
    explanation TEXT,
    better_native_alternative TEXT,
    fluency_score DOUBLE PRECISION
);

-- 25. Pronunciation Challenges & Attempts
CREATE TABLE IF NOT EXISTS pronunciation_challenges (
    id BIGSERIAL PRIMARY KEY,
    language VARCHAR(64) NOT NULL,
    cefr_level VARCHAR(16) NOT NULL,
    category VARCHAR(64) NOT NULL,
    phrase_text TEXT NOT NULL,
    phonetic_guide TEXT,
    translation TEXT NOT NULL,
    phonetic_tips TEXT,
    difficulty_score INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS pronunciation_attempts (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    challenge_id BIGINT NOT NULL REFERENCES pronunciation_challenges(id) ON DELETE CASCADE,
    spoken_text TEXT NOT NULL,
    accuracy_score DOUBLE PRECISION NOT NULL,
    completeness_score DOUBLE PRECISION NOT NULL,
    final_score DOUBLE PRECISION NOT NULL,
    word_breakdown_json TEXT,
    feedback_summary TEXT,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 26. Class Announcements
CREATE TABLE IF NOT EXISTS class_announcements (
    id BIGSERIAL PRIMARY KEY,
    school_class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 27. Teacher Student Notes
CREATE TABLE IF NOT EXISTS teacher_student_notes (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    school_class_id BIGINT REFERENCES classes(id) ON DELETE SET NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 28. Community Submissions, Reviews & Reports
CREATE TABLE IF NOT EXISTS community_submissions (
    id BIGSERIAL PRIMARY KEY,
    author_student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    author_name VARCHAR(255) NOT NULL,
    language VARCHAR(64) NOT NULL,
    prompt_title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    audio_url VARCHAR(256),
    target_level VARCHAR(16) DEFAULT 'A2',
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    review_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS community_reviews (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL REFERENCES community_submissions(id) ON DELETE CASCADE,
    reviewer_student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    reviewer_name VARCHAR(255) NOT NULL,
    reviewer_fluency_level VARCHAR(32) NOT NULL,
    overall_feedback TEXT NOT NULL,
    corrected_content TEXT NOT NULL,
    native_speaker_tips TEXT,
    rating INTEGER NOT NULL DEFAULT 5,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS community_reports (
    id BIGSERIAL PRIMARY KEY,
    reported_review_id BIGINT NOT NULL REFERENCES community_reviews(id) ON DELETE CASCADE,
    reported_by_student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    reason VARCHAR(64) NOT NULL,
    details TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
