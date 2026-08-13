package com.azasyu.domain.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azasyu.domain.auth.AuthService;
import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.domain.project.dto.CreateProjectRequest;
import com.azasyu.domain.project.dto.JoinProjectRequest;
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

        var created = projectService.create(ownerId, new CreateProjectRequest("해커톤", "가짜 합의를 줄이는 프로젝트"));
        var joined = projectService.join(memberId, new JoinProjectRequest(created.joinCode()));

        assertThat(created.myRole()).isEqualTo("OWNER");
        assertThat(joined.myRole()).isEqualTo("MEMBER");
        assertThat(joined.members()).hasSize(2);
        assertThat(projectService.getMyProjects(memberId)).hasSize(1);
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
