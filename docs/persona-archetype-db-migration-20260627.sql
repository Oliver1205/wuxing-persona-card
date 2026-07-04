-- Run once before deploying the 120-persona archetype engine to an existing MySQL database.
-- New local/test databases already get these columns from backend/src/main/resources/db/schema.sql.

ALTER TABLE user_result
    ADD COLUMN persona_type_id VARCHAR(96) NULL AFTER all_element_scores_json,
    ADD COLUMN accent_element VARCHAR(32) NULL AFTER persona_type_id,
    ADD COLUMN relation_kind VARCHAR(32) NULL AFTER accent_element,
    ADD COLUMN persona_label VARCHAR(96) NULL AFTER relation_kind,
    ADD COLUMN day_master_text TEXT NULL AFTER persona_label,
    ADD COLUMN primary_secondary_text TEXT NULL AFTER day_master_text,
    ADD COLUMN accent_text TEXT NULL AFTER primary_secondary_text,
    ADD COLUMN heaven_text TEXT NULL AFTER accent_text,
    ADD COLUMN human_text TEXT NULL AFTER heaven_text,
    ADD COLUMN star_officer_text TEXT NULL AFTER human_text,
    ADD COLUMN growth_advice_json TEXT NULL AFTER star_officer_text;

CREATE INDEX idx_persona_type_id ON user_result(persona_type_id);
