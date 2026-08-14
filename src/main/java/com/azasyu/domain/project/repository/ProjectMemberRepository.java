package com.azasyu.domain.project.repository;

import com.azasyu.domain.project.entity.ProjectMember;
import java.util.Collection;
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

    /**
     * 여러 프로젝트의 구성원을 한 번에 조회함.
     *
     * <p>목록 API에서 프로젝트마다 구성원을 따로 조회하면 N+1이 되므로 한 쿼리로 묶음.
     */
    @EntityGraph(attributePaths = "user")
    List<ProjectMember> findAllByProjectIdInOrderByJoinedAtAsc(Collection<Long> projectIds);
}
