package com.azasyu.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azasyu.domain.auth.AuthService;
import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.domain.meeting.dto.CreateMeetingRequest;
import com.azasyu.domain.project.service.ProjectService;
import com.azasyu.domain.project.dto.CreateProjectRequest;
import com.azasyu.global.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MeetingRecordServiceTest {

    @Autowired private MeetingRecordService recordService;
    @Autowired private MeetingService meetingService;
    @Autowired private MeetingAnalysisService analysisService;
    @Autowired private ProjectService projectService;
    @Autowired private AuthService authService;

    @Test
    void savesDirectText() {
        TestContext context = prepare("record-text@example.com");

        var record = recordService.createFromText(
            context.userId(), context.meetingId(), "핵심 기능을 먼저 구현하기로 결정했다."
        );

        assertThat(record.sourceType()).isEqualTo("TEXT");
        assertThat(recordService.get(context.userId(), context.meetingId()).content())
            .contains("핵심 기능");
        assertThat(analysisService.get(context.userId(), context.meetingId()).status())
            .isEqualTo("NOT_CONFIGURED");
    }

    @Test
    void extractsTxtAndRejectsSecondRecord() {
        TestContext context = prepare("record-file@example.com");
        var file = new MockMultipartFile(
            "file", "meeting.txt", "text/plain",
            "다음 주까지 데모를 완성한다.".getBytes(StandardCharsets.UTF_8)
        );

        var record = recordService.createFromFile(context.userId(), context.meetingId(), file);

        assertThat(record.sourceType()).isEqualTo("TXT");
        assertThat(record.originalFileName()).isEqualTo("meeting.txt");
        assertThatThrownBy(() -> recordService.createFromText(context.userId(), context.meetingId(), "중복"))
            .isInstanceOf(ApiException.class)
            .hasMessage("회의 내용이 이미 등록되어 있습니다.");
    }

    private TestContext prepare(String email) {
        Long userId = authService.signUp(new SignUpRequest(email, "참여자", "password123")).userId();
        var project = projectService.create(userId, new CreateProjectRequest("기록 프로젝트", "회의 내용 테스트"));
        var meeting = meetingService.create(userId, project.id(), new CreateMeetingRequest(
            "회의 기록", "결정 내용 기록", List.of("MVP 범위"), LocalDate.now().plusDays(1),
            LocalTime.of(17, 0), 60, List.of(userId)
        ));
        return new TestContext(userId, meeting.id());
    }

    private record TestContext(Long userId, Long meetingId) {
    }
}
