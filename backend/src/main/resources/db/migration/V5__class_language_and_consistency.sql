-- Migration V5: Add explicit class language consistency & enforce canonical 6 base languages

ALTER TABLE classes
    ADD COLUMN IF NOT EXISTS language_name VARCHAR(64);

UPDATE classes
SET language_name = CASE
    WHEN LOWER(name) LIKE '%alem%' THEN 'Alemão'
    WHEN LOWER(name) LIKE '%espanh%' THEN 'Espanhol'
    WHEN LOWER(name) LIKE '%franc%' THEN 'Francês'
    WHEN LOWER(name) LIKE '%ital%' THEN 'Italiano'
    WHEN LOWER(name) LIKE '%japon%' THEN 'Japonês'
    ELSE 'Inglês'
END
WHERE language_name IS NULL OR TRIM(language_name) = '';

ALTER TABLE classes
    ALTER COLUMN language_name SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_classes_language_name ON classes(language_name);

-- Remove any legacy non-canonical language catalog entries outside the 6 official platform languages
DELETE FROM catalog_options
WHERE LOWER(catalog_type) = 'language'
  AND LOWER(option_value) NOT IN ('alemão', 'alemao', 'espanhol', 'francês', 'frances', 'inglês', 'ingles', 'italiano', 'japonês', 'japones');
