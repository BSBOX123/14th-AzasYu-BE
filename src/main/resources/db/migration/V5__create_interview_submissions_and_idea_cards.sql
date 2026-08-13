CREATE TABLE interview_submissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    card_generation_status VARCHAR(30) NOT NULL,
    failure_message VARCHAR(500),
    submitted_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_interview_submissions PRIMARY KEY (id),
    CONSTRAINT uk_interview_submissions_meeting_user UNIQUE (meeting_id, user_id),
    CONSTRAINT fk_interview_submissions_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_interview_submissions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE interview_answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    CONSTRAINT pk_interview_answers PRIMARY KEY (id),
    CONSTRAINT uk_interview_answers_submission_question UNIQUE (submission_id, question_id),
    CONSTRAINT fk_interview_answers_submission FOREIGN KEY (submission_id) REFERENCES interview_submissions (id),
    CONSTRAINT fk_interview_answers_question FOREIGN KEY (question_id) REFERENCES interview_questions (id)
);

CREATE TABLE idea_cards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    core_opinion VARCHAR(1000) NOT NULL,
    rationale VARCHAR(2000) NOT NULL,
    concern VARCHAR(2000) NOT NULL,
    alternative VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_idea_cards PRIMARY KEY (id),
    CONSTRAINT uk_idea_cards_submission UNIQUE (submission_id),
    CONSTRAINT fk_idea_cards_submission FOREIGN KEY (submission_id) REFERENCES interview_submissions (id)
);
