package com.azasyu.domain.project;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    @EntityGraph(attributePaths = "project")
    List<ProjectMember> findAllByUserIdOrderByJoinedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"project", "user"})
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<ProjectMember> findAllByProjectIdOrderByJoinedAtAsc(Long projectId);
}
