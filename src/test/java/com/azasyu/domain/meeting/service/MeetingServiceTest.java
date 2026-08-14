package com.azasyu.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azasyu.domain.auth.AuthService;
import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.domain.interview.service.InterviewQuestionService;
import com.azasyu.domain.meeting.dto.CreateMeetingRequest;
import com.azasyu.domain.meeting.dto.JoinMeetingRequest;
import com.azasyu.domain.project.dto.CreateProjectRequest;
import com.azasyu.domain.project.dto.JoinProjectRequest;
import com.azasyu.domain.project.service.ProjectService;
import com.azasyu.global.error.ApiException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
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
    void createsMeetingAloneWhenNoParticipantGiven() {
        Long ownerId = signUp("solo-owner@example.com", "생성자");
        var project = projectService.create(ownerId, new CreateProjectRequest("혼자 회의", "참여자 미지정"));

        var created = meetingService.create(ownerId, project.id(), request(List.of()));

        assertThat(created.participants())
            .as("참여자를 지정하지 않아도 생성자는 참여자로 등록돼야 한다")
            .extracting("userId")
            .containsExactly(ownerId);
    }

    @Test
    void addsCreatorAsParticipantEvenWhenNotListed() {
        Long ownerId = signUp("absent-owner@example.com", "생성자");
        Long memberId = signUp("listed-member@example.com", "참여자");
        var project = projectService.create(ownerId, new CreateProjectRequest("생성자 누락", "자동 포함 확인"));
        projectService.join(memberId, new JoinProjectRequest(project.joinCode()));

        // 생성자를 빼고 팀원만 지정한다.
        var created = meetingService.create(ownerId, project.id(), request(List.of(memberId)));

        assertThat(created.participants()).extracting("userId")
            .containsExactlyInAnyOrder(ownerId, memberId);
        // 참여자여야 회의 원문·인터뷰 기능을 쓸 수 있다.
        assertThat(interviewQuestionService.getQuestions(ownerId, created.id()).generationStatus())
            .isEqualTo("NOT_CONFIGURED");
    }

    @Test
    void doesNotDuplicateCreatorWhenListedExplicitly() {
        Long ownerId = signUp("dup-owner@example.com", "생성자");
        var project = projectService.create(ownerId, new CreateProjectRequest("중복 확인", "생성자 명시"));

        var created = meetingService.create(ownerId, project.id(), request(List.of(ownerId, ownerId)));

        assertThat(created.participants()).hasSize(1);
    }

    @Test
    void projectMemberJoinsMeetingWithCode() {
        Long ownerId = signUp("code-owner@example.com", "생성자");
        Long memberId = signUp("code-member@example.com", "합류자");
        var project = projectService.create(ownerId, new CreateProjectRequest("코드 합류", "회의 코드"));
        projectService.join(memberId, new JoinProjectRequest(project.joinCode()));
        var meeting = meetingService.create(ownerId, project.id(), request(List.of()));

        // 소문자로 보내도 합류돼야 한다.
        var joined = meetingService.joinByCode(
            memberId, new JoinMeetingRequest(meeting.joinCode().toLowerCase(Locale.ROOT)));

        assertThat(joined.participants()).extracting("userId")
            .containsExactlyInAnyOrder(ownerId, memberId);
        // 참여자가 됐으므로 회의 기능을 쓸 수 있다.
        assertThat(interviewQuestionService.getQuestions(memberId, meeting.id()).generationStatus())
            .isEqualTo("NOT_CONFIGURED");
    }

    @Test
    void rejectsMeetingJoinFromNonProjectMember() {
        Long ownerId = signUp("code-owner2@example.com", "생성자");
        Long outsiderId = signUp("code-outsider@example.com", "외부인");
        var project = projectService.create(ownerId, new CreateProjectRequest("코드 거부", "구성원 아님"));
        var meeting = meetingService.create(ownerId, project.id(), request(List.of()));

        assertThatThrownBy(() -> meetingService.joinByCode(outsiderId, new JoinMeetingRequest(meeting.joinCode())))
            .isInstanceOf(ApiException.class)
            .hasMessage("프로젝트에 먼저 참여해야 회의에 합류할 수 있습니다.");
    }

    @Test
    void rejectsDuplicateMeetingJoin() {
        Long ownerId = signUp("code-owner3@example.com", "생성자");
        var project = projectService.create(ownerId, new CreateProjectRequest("중복 합류", "이미 참여자"));
        var meeting = meetingService.create(ownerId, project.id(), request(List.of()));

        // 생성자는 이미 참여자다.
        assertThatThrownBy(() -> meetingService.joinByCode(ownerId, new JoinMeetingRequest(meeting.joinCode())))
            .isInstanceOf(ApiException.class)
            .hasMessage("이미 참여 중인 회의입니다.");
    }

    @Test
    void rejectsUnknownMeetingJoinCode() {
        Long userId = signUp("code-unknown@example.com", "사용자");

        assertThatThrownBy(() -> meetingService.joinByCode(userId, new JoinMeetingRequest("ZZZZ9999")))
            .isInstanceOf(ApiException.class)
            .hasMessage("유효하지 않은 회의 참여 코드입니다.");
    }

    @Test
    void meetingsHaveDistinctJoinCodes() {
        Long ownerId = signUp("code-distinct@example.com", "생성자");
        var project = projectService.create(ownerId, new CreateProjectRequest("코드 중복", "유일성"));

        var first = meetingService.create(ownerId, project.id(), request(List.of()));
        var second = meetingService.create(ownerId, project.id(), request(List.of()));

        assertThat(first.joinCode()).isNotBlank().hasSize(8).isNotEqualTo(second.joinCode());
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
