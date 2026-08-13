package com.azasyu.domain.meeting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azasyu.domain.auth.AuthService;
import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.domain.interview.InterviewQuestionService;
import com.azasyu.domain.meeting.dto.CreateMeetingRequest;
import com.azasyu.domain.project.ProjectService;
import com.azasyu.domain.project.dto.CreateProjectRequest;
import com.azasyu.domain.project.dto.JoinProjectRequest;
import com.azasyu.global.error.ApiException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MeetingServiceTest {

    @Autowired private MeetingService meetingService;
    @Autowired private ProjectService projectService;
    @Autowired private AuthService authService;
    @Autowired private InterviewQuestionService interviewQuestionService;

    @Test
    void projectMemberCreatesMeetingWithAgendasAndParticipants() {
        Long ownerId = signUp("meeting-owner@example.com", "생성자");
        Long memberId = signUp("meeting-member@example.com", "참여자");
        var project = projectService.create(ownerId, new CreateProjectRequest("회의 프로젝트", "회의 테스트"));
        projectService.join(memberId, new JoinProjectRequest(project.joinCode()));

        var created = meetingService.create(ownerId, project.id(), request(List.of(ownerId, memberId)));

        assertThat(created.agendas()).extracting("content")
            .containsExactly("핵심 기능 범위", "발표 시나리오");
        assertThat(created.participants()).hasSize(2);
        assertThat(meetingService.getProjectMeetings(memberId, project.id())).hasSize(1);
        assertThat(interviewQuestionService.getQuestions(memberId, created.id()).generationStatus())
            .isEqualTo("NOT_CONFIGURED");
    }

    @Test
    void rejectsParticipantOutsideProject() {
        Long ownerId = signUp("meeting-owner2@example.com", "생성자");
        Long outsiderId = signUp("meeting-outsider@example.com", "외부인");
        var project = projectService.create(ownerId, new CreateProjectRequest("회의 프로젝트2", "참여자 검증"));

        assertThatThrownBy(() -> meetingService.create(ownerId, project.id(), request(List.of(outsiderId))))
            .isInstanceOf(ApiException.class)
            .hasMessage("프로젝트 구성원이 아닌 사용자는 회의 참여자로 선택할 수 없습니다.");
    }

    private CreateMeetingRequest request(List<Long> participantIds) {
        return new CreateMeetingRequest(
            "MVP 기능 결정", "해커톤 구현 범위를 확정한다.",
            List.of("핵심 기능 범위", "발표 시나리오"), LocalDate.now().plusDays(1),
            LocalTime.of(14, 0), 60, participantIds
        );
    }

    private Long signUp(String email, String name) {
        return authService.signUp(new SignUpRequest(email, name, "password123")).userId();
    }
}
