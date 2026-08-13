CREATE TABLE meeting_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    original_file_name VARCHAR(255),
    content LONGTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_meeting_records PRIMARY KEY (id),
    CONSTRAINT uk_meeting_records_meeting UNIQUE (meeting_id),
    CONSTRAINT fk_meeting_records_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_meeting_records_user FOREIGN KEY (created_by) REFERENCES users (id)
);
