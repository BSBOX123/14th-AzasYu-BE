package com.azasyu.domain.project.repository;

import com.azasyu.domain.project.entity.Project;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByJoinCode(String joinCode);

    Optional<Project> findByJoinCode(String joinCode);
}
