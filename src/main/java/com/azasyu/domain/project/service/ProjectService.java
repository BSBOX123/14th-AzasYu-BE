package com.azasyu.domain.project.service;

import com.azasyu.domain.project.dto.CreateProjectRequest;
import com.azasyu.domain.project.dto.JoinProjectRequest;
import com.azasyu.domain.project.dto.MemberResponse;
import com.azasyu.domain.project.dto.ProjectDetailResponse;
import com.azasyu.domain.project.dto.ProjectSummaryResponse;
import com.azasyu.domain.project.entity.Project;
import com.azasyu.domain.project.entity.ProjectMember;
import com.azasyu.domain.project.entity.ProjectMemberRole;
import com.azasyu.domain.project.repository.ProjectMemberRepository;
import com.azasyu.domain.project.repository.ProjectRepository;
import com.azasyu.domain.user.User;
import com.azasyu.domain.user.UserRepository;
import com.azasyu.global.error.ApiException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로젝트 생성과 참여 코드 기반 참가.
 *
 * <p>구성원이 아닌 사용자에게는 존재 여부를 숨기기 위해 403이 아니라 404를 반환함.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final int JOIN_CODE_GENERATION_ATTEMPTS = 10;

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final JoinCodeGenerator joinCodeGenerator;

    @Transactional
    public ProjectDetailResponse create(Long userId, CreateProjectRequest request) {
        User owner = getUser(userId);
        Project project = projectRepository.save(new Project(
            request.name().trim(), request.description().trim(), createUniqueJoinCode()
        ));
        projectMemberRepository.save(new ProjectMember(project, owner, ProjectMemberRole.OWNER));
        return getDetail(userId, project.getId());
    }

    @Transactional
    public ProjectDetailResponse join(Long userId, JoinProjectRequest request) {
        String joinCode = request.joinCode().trim().toUpperCase(Locale.ROOT);
        Project project = projectRepository.findByJoinCode(joinCode)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JOIN_CODE_NOT_FOUND", "유효하지 않은 참여 코드입니다."));

        if (projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_PROJECT_MEMBER", "이미 참여 중인 프로젝트입니다.");
        }

        projectMemberRepository.save(new ProjectMember(project, getUser(userId), ProjectMemberRole.MEMBER));
        return getDetail(userId, project.getId());
    }

    /**
     * 내가 참여 중인 프로젝트를 최근 참여 순으로 반환함.
     *
     * <p>구성원 목록까지 담으므로 목록 화면에서 상세를 다시 호출할 필요가 없음.
     * 프로젝트마다 구성원을 따로 조회하면 N+1이 되므로 한 쿼리로 묶어 조회한 뒤 묶음.
     */
    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> getMyProjects(Long userId) {
        List<ProjectMember> myMemberships = projectMemberRepository.findAllByUserIdOrderByJoinedAtDesc(userId);
        if (myMemberships.isEmpty()) {
            return List.of();
        }

        List<Long> projectIds = myMemberships.stream().map(member -> member.getProject().getId()).toList();
        Map<Long, List<MemberResponse>> membersByProject = projectMemberRepository
            .findAllByProjectIdInOrderByJoinedAtAsc(projectIds).stream()
            .collect(Collectors.groupingBy(
                member -> member.getProject().getId(),
                LinkedHashMap::new,
                Collectors.mapping(this::toMemberResponse, Collectors.toList())
            ));

        return myMemberships.stream()
            .map(membership -> {
                Project project = membership.getProject();
                return new ProjectSummaryResponse(
                    project.getId(),
                    project.getName(),
                    project.getDescription(),
                    membership.getRole(),
                    membership.getJoinedAt(),
                    project.getCreatedAt(),
                    membersByProject.getOrDefault(project.getId(), List.of())
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getDetail(Long userId, Long projectId) {
        ProjectMember currentMember = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."));

        Project project = currentMember.getProject();
        List<MemberResponse> members = projectMemberRepository
            .findAllByProjectIdOrderByJoinedAtAsc(projectId).stream()
            .map(this::toMemberResponse)
            .toList();

        return new ProjectDetailResponse(
            project.getId(), project.getName(), project.getDescription(), project.getJoinCode(),
            currentMember.getRole(), project.getCreatedAt(), members
        );
    }

    private MemberResponse toMemberResponse(ProjectMember member) {
        return new MemberResponse(
            member.getUser().getId(), member.getUser().getName(), member.getRole(), member.getJoinedAt()
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "인증된 사용자를 찾을 수 없습니다."));
    }

    /**
     * 중복되지 않는 참여 코드를 만듦.
     *
     * <p>충돌 시 최대 {@value #JOIN_CODE_GENERATION_ATTEMPTS}회까지 재시도하고,
     * 그래도 실패하면 예외를 던짐.
     */
    private String createUniqueJoinCode() {
        for (int attempt = 0; attempt < JOIN_CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = joinCodeGenerator.generate();
            if (!projectRepository.existsByJoinCode(code)) {
                return code;
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "JOIN_CODE_GENERATION_FAILED", "참여 코드 생성에 실패했습니다.");
    }
}
