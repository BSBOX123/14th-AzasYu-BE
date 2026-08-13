package com.azasyu.domain.project;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByJoinCode(String joinCode);

    Optional<Project> findByJoinCode(String joinCode);
}
