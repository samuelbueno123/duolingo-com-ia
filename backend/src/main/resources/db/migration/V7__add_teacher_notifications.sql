-- Teacher persistent notifications (pending corrections and classroom alerts)
CREATE TABLE IF NOT EXISTS teacher_notifications (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    action_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_teacher_notif_teacher ON teacher_notifications(teacher_id);
CREATE INDEX IF NOT EXISTS idx_teacher_notif_read ON teacher_notifications(teacher_id, is_read);
