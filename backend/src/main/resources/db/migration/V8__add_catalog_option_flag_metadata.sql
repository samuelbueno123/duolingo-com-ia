-- Adiciona suporte a bandeira automatica ou manual nas opcoes de idioma do catalogo
ALTER TABLE catalog_options ADD COLUMN IF NOT EXISTS flag_mode VARCHAR(20) NOT NULL DEFAULT 'AUTO';
ALTER TABLE catalog_options ADD COLUMN IF NOT EXISTS flag_code VARCHAR(32);
ALTER TABLE catalog_options ADD COLUMN IF NOT EXISTS flag_url VARCHAR(500);
