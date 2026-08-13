CREATE TABLE projects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    join_code VARCHAR(8) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT uk_projects_join_code UNIQUE (join_code)
);

CREATE TABLE project_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_project_members PRIMARY KEY (id),
    CONSTRAINT uk_project_members_project_user UNIQUE (project_id, user_id),
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_members_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_project_members_user_id ON project_members (user_id);
