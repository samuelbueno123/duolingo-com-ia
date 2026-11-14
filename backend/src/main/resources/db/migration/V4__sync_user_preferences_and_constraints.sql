-- Sincroniza colunas de preferencias e notificacoes do usuario que existem na entidade JPA User
-- Garantido idempotente para bases existentes ou novas.

ALTER TABLE users ADD COLUMN IF NOT EXISTS ui_language VARCHAR(10);
ALTER TABLE users ADD COLUMN IF NOT EXISTS theme_mode VARCHAR(10);
ALTER TABLE users ADD COLUMN IF NOT EXISTS daily_goal_minutes INTEGER;
ALTER TABLE users ADD COLUMN IF NOT EXISTS notif_daily_reminders BOOLEAN;
ALTER TABLE users ADD COLUMN IF NOT EXISTS notif_achievements BOOLEAN;
ALTER TABLE users ADD COLUMN IF NOT EXISTS notif_ranking BOOLEAN;
ALTER TABLE users ADD COLUMN IF NOT EXISTS notif_class_updates BOOLEAN;
