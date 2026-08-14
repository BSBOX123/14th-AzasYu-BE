package com.azasyu.domain.project.service;

import com.azasyu.domain.project.dto.CreateProjectRequest;
import com.azasyu.domain.project.dto.JoinProjectRequest;
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
import java.util.List;
import java.util.Locale;
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

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> getMyProjects(Long userId) {
        return projectMemberRepository.findAllByUserIdOrderByJoinedAtDesc(userId).stream()
            .map(member -> new ProjectSummaryResponse(
                member.getProject().getId(),
                member.getProject().getName(),
                member.getProject().getDescription(),
                member.getRole(),
                member.getJoinedAt()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getDetail(Long userId, Long projectId) {
        ProjectMember currentMember = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."));

        Project project = currentMember.getProject();
        List<ProjectDetailResponse.MemberResponse> members = projectMemberRepository
            .findAllByProjectIdOrderByJoinedAtAsc(projectId).stream()
            .map(member -> new ProjectDetailResponse.MemberResponse(
                member.getUser().getId(), member.getUser().getName(), member.getRole(), member.getJoinedAt()
            ))
            .toList();

        return new ProjectDetailResponse(
            project.getId(), project.getName(), project.getDescription(), project.getJoinCode(),
            currentMember.getRole(), project.getCreatedAt(), members
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
