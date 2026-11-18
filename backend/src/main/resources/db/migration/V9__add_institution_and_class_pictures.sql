-- Adiciona suporte a foto personalizada para instituicoes e turmas
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS picture_url VARCHAR(2048);
ALTER TABLE classes ADD COLUMN IF NOT EXISTS picture_url VARCHAR(2048);
