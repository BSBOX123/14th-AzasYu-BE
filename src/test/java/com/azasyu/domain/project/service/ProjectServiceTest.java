package com.azasyu.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.azasyu.domain.auth.AuthService;
import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.domain.project.dto.CreateProjectRequest;
import com.azasyu.domain.project.dto.JoinProjectRequest;
import com.azasyu.domain.project.dto.MemberResponse;
import com.azasyu.domain.project.entity.ProjectColor;
import com.azasyu.domain.project.repository.ProjectMemberRepository;
import com.azasyu.domain.project.repository.ProjectRepository;
import com.azasyu.global.error.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProjectServiceTest {

    @Autowired private ProjectService projectService;
    @Autowired private AuthService authService;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private ProjectRepository projectRepository;

    @Test
    void createsProjectAndJoinsWithCode() {
        Long ownerId = signUp("owner@example.com", "생성자");
        Long memberId = signUp("member@example.com", "참여자");

        var created = projectService.create(ownerId, new CreateProjectRequest(
            "해커톤", "가짜 합의를 줄이는 프로젝트", ProjectColor.ORANGE
        ));
        var joined = projectService.join(memberId, new JoinProjectRequest(created.joinCode()));

        assertThat(created.myRole()).isEqualTo("OWNER");
        assertThat(created.color()).isEqualTo(ProjectColor.ORANGE);
        assertThat(joined.myRole()).isEqualTo("MEMBER");
        assertThat(joined.color()).isEqualTo(ProjectColor.ORANGE);
        assertThat(joined.members()).hasSize(2);
        assertThat(projectService.getMyProjects(memberId)).hasSize(1);
    }

    @Test
    void projectListIncludesMembersAndDates() {
        Long ownerId = signUp("list-owner@example.com", "생성자");
        Long memberId = signUp("list-member@example.com", "참여자");
        var created = projectService.create(ownerId, new CreateProjectRequest("목록 확인", "구성원 포함 여부"));
        projectService.join(memberId, new JoinProjectRequest(created.joinCode()));

        var summary = projectService.getMyProjects(memberId).getFirst();

        assertThat(summary.members())
            .extracting(MemberResponse::name, MemberResponse::role)
            .containsExactly(tuple("생성자", "OWNER"), tuple("참여자", "MEMBER"));
        assertThat(summary.members()).allSatisfy(member -> assertThat(member.joinedAt()).isNotNull());
        assertThat(summary.createdAt()).isNotNull();
        assertThat(summary.joinedAt()).isNotNull();
        assertThat(summary.color()).isEqualTo(ProjectColor.BLUE);
        // 목록만으로 상세 화면을 그릴 수 있어야 한다. 참여 코드만 상세 전용이다.
        assertThat(summary.members()).hasSameSizeAs(projectService.getDetail(memberId, created.id()).members());
    }

    @Test
    void projectListReturnsEmptyForUserWithoutProjects() {
        Long lonelyUserId = signUp("no-project@example.com", "미참여자");

        assertThat(projectService.getMyProjects(lonelyUserId)).isEmpty();
    }

    @Test
    void hidesProjectFromNonMember() {
        Long ownerId = signUp("owner2@example.com", "생성자");
        Long outsiderId = signUp("outsider@example.com", "외부인");
        var created = projectService.create(ownerId, new CreateProjectRequest("비공개", "구성원만 조회"));

        assertThatThrownBy(() -> projectService.getDetail(outsiderId, created.id()))
            .isInstanceOf(ApiException.class)
            .hasMessage("프로젝트를 찾을 수 없습니다.");
    }

    private Long signUp(String email, String name) {
        return authService.signUp(new SignUpRequest(email, name, "password123")).userId();
    }
}
