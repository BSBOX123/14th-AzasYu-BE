ALTER TABLE idea_cards
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

UPDATE idea_cards
SET updated_at = created_at;

ALTER TABLE idea_cards
    ADD COLUMN visible BOOLEAN NOT NULL DEFAULT TRUE;
