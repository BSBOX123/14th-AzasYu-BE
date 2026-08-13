CREATE TABLE idea_summaries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    refreshed_by BIGINT NOT NULL,
    version INT NOT NULL,
    source_card_count INT NOT NULL,
    common_opinions VARCHAR(4000) NOT NULL,
    differing_opinions VARCHAR(4000) NOT NULL,
    key_concerns VARCHAR(4000) NOT NULL,
    discussion_points VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_idea_summaries PRIMARY KEY (id),
    CONSTRAINT uk_idea_summaries_meeting_version UNIQUE (meeting_id, version),
    CONSTRAINT fk_idea_summaries_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_idea_summaries_user FOREIGN KEY (refreshed_by) REFERENCES users (id)
);
