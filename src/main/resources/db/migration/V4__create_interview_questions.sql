CREATE TABLE interview_question_sets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_message VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_interview_question_sets PRIMARY KEY (id),
    CONSTRAINT uk_interview_question_sets_meeting UNIQUE (meeting_id),
    CONSTRAINT fk_interview_question_sets_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id)
);

CREATE TABLE interview_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_set_id BIGINT NOT NULL,
    question_order INT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    CONSTRAINT pk_interview_questions PRIMARY KEY (id),
    CONSTRAINT uk_interview_questions_order UNIQUE (question_set_id, question_order),
    CONSTRAINT fk_interview_questions_set FOREIGN KEY (question_set_id) REFERENCES interview_question_sets (id)
);
