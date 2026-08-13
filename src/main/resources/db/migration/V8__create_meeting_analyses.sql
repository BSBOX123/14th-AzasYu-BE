CREATE TABLE meeting_analyses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_message VARCHAR(500),
    meeting_purpose VARCHAR(4000),
    key_discussions TEXT,
    decisions TEXT,
    follow_up_checks TEXT,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_meeting_analyses PRIMARY KEY (id),
    CONSTRAINT uk_meeting_analyses_meeting UNIQUE (meeting_id),
    CONSTRAINT fk_meeting_analyses_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id)
);

CREATE TABLE ambiguity_findings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    analysis_id BIGINT NOT NULL,
    finding_order INT NOT NULL,
    expression VARCHAR(2000) NOT NULL,
    reason VARCHAR(4000) NOT NULL,
    CONSTRAINT pk_ambiguity_findings PRIMARY KEY (id),
    CONSTRAINT uk_ambiguity_findings_order UNIQUE (analysis_id, finding_order),
    CONSTRAINT fk_ambiguity_findings_analysis FOREIGN KEY (analysis_id) REFERENCES meeting_analyses (id)
);
