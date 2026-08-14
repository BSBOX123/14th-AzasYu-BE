package com.azasyu.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.azasyu.domain.auth.AuthService;
import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.domain.interview.service.InterviewQuestionService;
import com.azasyu.domain.interview.service.InterviewSubmissionService;
import com.azasyu.domain.interview.ai.IdeaCardAiClient;
import com.azasyu.domain.interview.ai.IdeaCardDraft;
import com.azasyu.domain.interview.ai.InterviewAnswerContext;
import com.azasyu.domain.interview.ai.InterviewQuestionAiClient;
import com.azasyu.domain.interview.dto.SubmitInterviewRequest;
import com.azasyu.domain.meeting.service.MeetingAnalysisService;
import com.azasyu.domain.meeting.service.MeetingRecordService;
import com.azasyu.domain.meeting.service.MeetingService;
import com.azasyu.domain.meeting.ai.MeetingAnalysisAiClient;
import com.azasyu.domain.meeting.ai.MeetingAnalysisDraft;
import com.azasyu.domain.meeting.ai.MeetingContext;
import com.azasyu.domain.meeting.dto.CreateMeetingRequest;
import com.azasyu.domain.project.service.ProjectService;
import com.azasyu.domain.project.dto.CreateProjectRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * AI 호출이 DB 트랜잭션 밖에서 이루어지는지 검증한다.
 *
 * <p>운영진 피드백의 트랜잭션 분리 항목에 대한 회귀 테스트다. AI 클라이언트를
 * 스텁으로 바꿔 호출 시점에 활성 트랜잭션이 있는지 기록하고, 없어야 통과한다.
 *
 * <p>클래스에 {@code @Transactional}을 걸지 않는다. 테스트 트랜잭션이 열려 있으면
 * 서비스가 그 트랜잭션에 참여해 버려서 검증 자체가 무의미해지기 때문이다.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ai-tx-boundary;MODE=MySQL;DB_CLOSE_DELAY=-1",
    // 기본 테스트 설정은 create-drop이라 Hibernate가 만든 스키마를 쓴다. 엔티티에 선언되지
    // 않은 유니크 제약(uk_ambiguity_findings_order 등)이 빠져 실제 스키마와 달라지므로,
    // 이 테스트는 Flyway 마이그레이션이 만든 스키마를 그대로 사용한다.
    "spring.jpa.hibernate.ddl-auto=validate"
})
class AiCallTransactionBoundaryTest {

    @Autowired private AuthService authService;
    @Autowired private ProjectService projectService;
    @Autowired private MeetingService meetingService;
    @Autowired private InterviewQuestionService questionService;
    @Autowired private InterviewSubmissionService submissionService;
    @Autowired private MeetingRecordService recordService;
    @Autowired private MeetingAnalysisService analysisService;

    @Autowired private TransactionProbe probe;

    @Test
    void aiClientsAreCalledOutsideTransaction() {
        Long ownerId = signUp("tx-boundary-owner@example.com", "생성자");
        var project = projectService.create(ownerId, new CreateProjectRequest("트랜잭션 검증", "경계 확인"));

        // 회의 생성 -> 공통 질문 생성 (MeetingService.create 호출부 포함)
        var meeting = meetingService.create(ownerId, project.id(), new CreateMeetingRequest(
            "경계 확인 회의", "AI 호출 경계를 확인한다.", List.of("안건 하나"),
            LocalDate.now().plusDays(1), LocalTime.of(10, 0), 60, List.of(ownerId)
        ));
        assertThat(probe.questionClientInTransaction)
            .as("공통 질문 생성이 호출되지 않았거나 트랜잭션 안에서 호출됨")
            .isFalse();

        // 인터뷰 답변 제출 -> 아이디어 카드 생성
        var questions = questionService.getQuestions(ownerId, meeting.id()).questions();
        submissionService.submit(ownerId, meeting.id(), new SubmitInterviewRequest(
            questions.stream()
                .map(question -> new SubmitInterviewRequest.AnswerRequest(question.id(), "답변 내용"))
                .toList()
        ));
        assertThat(probe.ideaCardClientInTransaction)
            .as("아이디어 카드 생성이 호출되지 않았거나 트랜잭션 안에서 호출됨")
            .isFalse();

        // 회의 원문 등록 -> 회의 분석 (MeetingRecordService 호출부 포함)
        recordService.createFromText(ownerId, meeting.id(), "회의 원문입니다. 다음 주까지 정리하기로 했습니다.");
        assertThat(probe.analysisClientInTransaction)
            .as("회의 분석이 호출되지 않았거나 트랜잭션 안에서 호출됨")
            .isFalse();
    }

    /**
     * 회의 분석을 재생성해도 기존 모호성 탐지 결과와 충돌하지 않아야 한다.
     *
     * <p>파생 삭제는 DELETE를 flush까지 미루고 Hibernate가 INSERT를 먼저 실행해,
     * 같은 {@code (analysis_id, finding_order)}로 유니크 제약을 위반했다.
     */
    @Test
    void analysisCanBeRegeneratedAfterSuccess() {
        Long ownerId = signUp("regenerate-owner@example.com", "재생성");
        var project = projectService.create(ownerId, new CreateProjectRequest("재생성 검증", "유니크 제약"));
        var meeting = meetingService.create(ownerId, project.id(), new CreateMeetingRequest(
            "재생성 회의", "재생성을 확인한다.", List.of("안건"),
            LocalDate.now().plusDays(1), LocalTime.of(11, 0), 60, List.of(ownerId)
        ));

        recordService.createFromText(ownerId, meeting.id(), "적당히 하기로 했고 조만간 정하기로 했습니다.");
        var first = analysisService.get(ownerId, meeting.id());
        assertThat(first.status()).isEqualTo("GENERATED");
        assertThat(first.ambiguities()).hasSize(2);

        var second = analysisService.retry(ownerId, meeting.id());

        assertThat(second.status())
            .as("재생성이 유니크 제약 위반으로 실패함")
            .isEqualTo("GENERATED");
        assertThat(second.ambiguities()).hasSize(2);
    }

    private Long signUp(String email, String name) {
        return authService.signUp(new SignUpRequest(email, name, "password123")).userId();
    }

    /** AI 클라이언트 호출 시점의 트랜잭션 활성 여부. null이면 호출 자체가 없었다는 뜻이다. */
    static class TransactionProbe {
        Boolean questionClientInTransaction;
        Boolean ideaCardClientInTransaction;
        Boolean analysisClientInTransaction;
    }

    @TestConfiguration
    static class StubAiClientConfig {

        @Bean
        TransactionProbe transactionProbe() {
            return new TransactionProbe();
        }

        @Bean
        @Primary
        InterviewQuestionAiClient stubQuestionClient(TransactionProbe probe) {
            return new InterviewQuestionAiClient() {
                @Override
                public boolean isConfigured() {
                    return true;
                }

                @Override
                public List<String> generate(MeetingContext meeting, List<String> agendas) {
                    probe.questionClientInTransaction = TransactionSynchronizationManager.isActualTransactionActive();
                    return List.of("첫 번째 질문", "두 번째 질문");
                }
            };
        }

        @Bean
        @Primary
        IdeaCardAiClient stubIdeaCardClient(TransactionProbe probe) {
            return new IdeaCardAiClient() {
                @Override
                public boolean isConfigured() {
                    return true;
                }

                @Override
                public IdeaCardDraft generate(MeetingContext meeting, List<InterviewAnswerContext> answers) {
                    probe.ideaCardClientInTransaction = TransactionSynchronizationManager.isActualTransactionActive();
                    return new IdeaCardDraft("핵심", "이유", "우려", "대안");
                }
            };
        }

        @Bean
        @Primary
        MeetingAnalysisAiClient stubAnalysisClient(TransactionProbe probe) {
            return new MeetingAnalysisAiClient() {
                @Override
                public boolean isConfigured() {
                    return true;
                }

                @Override
                public MeetingAnalysisDraft analyze(MeetingContext meeting, String recordContent) {
                    probe.analysisClientInTransaction = TransactionSynchronizationManager.isActualTransactionActive();
                    // 모호성을 반환해야 재생성 시 기존 findings 삭제 경로를 검증할 수 있다.
                    return new MeetingAnalysisDraft("목적", "논의", "결정", "확인", List.of(
                        new MeetingAnalysisDraft.AmbiguityDraft("적당히", "기준이 불명확함"),
                        new MeetingAnalysisDraft.AmbiguityDraft("조만간", "기한이 불명확함")
                    ));
                }
            };
        }
    }
}
