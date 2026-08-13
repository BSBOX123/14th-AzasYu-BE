CREATE TABLE meetings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    purpose VARCHAR(1000) NOT NULL,
    meeting_date DATE NOT NULL,
    start_time TIME NOT NULL,
    expected_duration_minutes INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_meetings PRIMARY KEY (id),
    CONSTRAINT fk_meetings_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_meetings_creator FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE INDEX idx_meetings_project_date ON meetings (project_id, meeting_date, start_time);

CREATE TABLE meeting_agendas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    agenda_order INT NOT NULL,
    content VARCHAR(500) NOT NULL,
    CONSTRAINT pk_meeting_agendas PRIMARY KEY (id),
    CONSTRAINT uk_meeting_agendas_order UNIQUE (meeting_id, agenda_order),
    CONSTRAINT fk_meeting_agendas_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id)
);

CREATE TABLE meeting_participants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT pk_meeting_participants PRIMARY KEY (id),
    CONSTRAINT uk_meeting_participants_meeting_user UNIQUE (meeting_id, user_id),
    CONSTRAINT fk_meeting_participants_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_meeting_participants_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_meeting_participants_user ON meeting_participants (user_id);
