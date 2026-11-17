-- Migration V6: Add optional teacher mascot help message to learning exercises

ALTER TABLE learning_exercises
    ADD COLUMN IF NOT EXISTS mascot_help_message TEXT;
