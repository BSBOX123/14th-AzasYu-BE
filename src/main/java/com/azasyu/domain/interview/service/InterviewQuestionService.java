package com.azasyu.domain.interview.service;

import com.azasyu.domain.interview.ai.InterviewQuestionAiClient;
import com.azasyu.domain.interview.dto.InterviewQuestionsResponse;
import com.azasyu.domain.interview.entity.InterviewQuestion;
import com.azasyu.domain.interview.entity.InterviewQuestionSet;
import com.azasyu.domain.interview.entity.QuestionGenerationStatus;
import com.azasyu.domain.interview.repository.InterviewQuestionRepository;
import com.azasyu.domain.interview.repository.InterviewQuestionSetRepository;
import com.azasyu.domain.meeting.ai.MeetingContext;
import com.azasyu.domain.meeting.entity.Meeting;
import com.azasyu.domain.meeting.entity.MeetingAgenda;
import com.azasyu.domain.meeting.repository.MeetingAgendaRepository;
import com.azasyu.domain.meeting.repository.MeetingParticipantRepository;
import com.azasyu.domain.meeting.repository.MeetingRepository;
import com.azasyu.domain.project.repository.ProjectMemberRepository;
import com.azasyu.global.ai.GeminiApiException;
import com.azasyu.global.error.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 회의 안건 기반 공통 질문 생성과 조회.
 *
 * <p>이미 GENERATED 상태면 재생성 요청을 무시함. 답변을 제출한 참여자가 있는데
 * 질문이 바뀌면 답변이 어긋나기 때문임.
 *
 * <p>조회는 회의 참여자만, 재생성은 프로젝트 구성원이면 가능함. *
 * <p>AI 호출은 트랜잭션 밖에서 수행함. 트랜잭션 안에서 호출하면 응답이 늦어질 때
 * DB 커넥션이 그만큼 오래 점유됨. 상태 저장은 {@code TransactionTemplate}으로
 * 짧은 트랜잭션을 열어 처리하므로 서비스 메서드에 {@code @Transactional}을 걸지 않음.
 */
@Service
@RequiredArgsConstructor
public class InterviewQuestionService {

    private static final Logger log = LoggerFactory.getLogger(InterviewQuestionService.class);

    private final InterviewQuestionSetRepository questionSetRepository;
    private final InterviewQuestionRepository questionRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingAgendaRepository agendaRepository;
    private final MeetingParticipantRepository participantRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final InterviewQuestionAiClient aiClient;
    private final TransactionTemplate transactionTemplate;

    /** 회의 생성 직후 공통 질문 생성을 시작함. */
    public InterviewQuestionsResponse initializeAndGenerate(Long meetingId) {
        PendingGeneration pending = transactionTemplate.execute(status -> {
            Meeting meeting = getMeeting(meetingId);
            InterviewQuestionSet questionSet = questionSetRepository.save(new InterviewQuestionSet(meeting));
            return toPendingGeneration(questionSet, meeting);
        });
        return generateAndSave(pending);
    }

    public InterviewQuestionsResponse retry(Long userId, Long meetingId) {
        Optional<PendingGeneration> pending = transactionTemplate.execute(status -> {
            Meeting meeting = getMeeting(meetingId);
            requireProjectMember(meeting, userId);
            InterviewQuestionSet questionSet = questionSetRepository.findByMeetingId(meetingId)
                .orElseGet(() -> questionSetRepository.save(new InterviewQuestionSet(meeting)));
            if (QuestionGenerationStatus.GENERATED.name().equals(questionSet.getStatus())) {
                return Optional.empty();
            }
            questionSet.pending();
            return Optional.of(toPendingGeneration(questionSet, meeting));
        });
        return pending
            .map(this::generateAndSave)
            .orElseGet(() -> readResponse(meetingId));
    }

    @Transactional(readOnly = true)
    public InterviewQuestionsResponse getQuestions(Long userId, Long meetingId) {
        if (!participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "INTERVIEW_NOT_FOUND", "참여 가능한 인터뷰를 찾을 수 없습니다.");
        }
        return readQuestionSet(meetingId);
    }

    private InterviewQuestionsResponse generateAndSave(PendingGeneration pending) {
        if (!aiClient.isConfigured()) {
            return updateQuestionSet(pending.questionSetId(), InterviewQuestionSet::notConfigured);
        }

        List<String> generatedQuestions;
        try {
            // 트랜잭션 밖에서 호출함. 응답이 지연돼도 DB 커넥션을 잡지 않음.
            generatedQuestions = aiClient.generate(pending.meeting(), pending.agendas());
        } catch (GeminiApiException exception) {
            log.warn("Gemini interview question generation failed: status={}, message={}",
                exception.getStatusCode(), exception.getMessage());
            return updateQuestionSet(pending.questionSetId(), questionSet -> questionSet.failed(exception.userMessage()));
        } catch (RuntimeException exception) {
            log.error("Interview question generation failed", exception);
            return updateQuestionSet(pending.questionSetId(),
                questionSet -> questionSet.failed("질문 생성에 실패했습니다. 잠시 후 다시 시도해 주세요."));
        }

        try {
            return transactionTemplate.execute(status -> {
                InterviewQuestionSet questionSet = getQuestionSet(pending.questionSetId());
                questionRepository.deleteAllByQuestionSetId(questionSet.getId());
                for (int index = 0; index < generatedQuestions.size(); index++) {
                    questionRepository.save(new InterviewQuestion(
                        questionSet, index + 1, generatedQuestions.get(index).trim()
                    ));
                }
                questionSet.generated();
                return toResponse(questionSet);
            });
        } catch (RuntimeException exception) {
            // 저장 단계가 실패하면 상태가 PENDING에 멈춤. FAILED로 내려 재시도할 수 있게 함.
            log.error("Interview question save failed: questionSetId={}", pending.questionSetId(), exception);
            return updateQuestionSet(pending.questionSetId(),
                questionSet -> questionSet.failed("질문 저장에 실패했습니다. 잠시 후 다시 시도해 주세요."));
        }
    }

    private InterviewQuestionsResponse updateQuestionSet(Long questionSetId, Consumer<InterviewQuestionSet> change) {
        return transactionTemplate.execute(status -> {
            InterviewQuestionSet questionSet = getQuestionSet(questionSetId);
            change.accept(questionSet);
            return toResponse(questionSet);
        });
    }

    private PendingGeneration toPendingGeneration(InterviewQuestionSet questionSet, Meeting meeting) {
        List<String> agendas = agendaRepository.findAllByMeetingIdOrderByAgendaOrderAsc(meeting.getId()).stream()
            .map(MeetingAgenda::getContent)
            .toList();
        return new PendingGeneration(questionSet.getId(), MeetingContext.from(meeting), agendas);
    }

    private InterviewQuestionsResponse readResponse(Long meetingId) {
        return transactionTemplate.execute(status -> readQuestionSet(meetingId));
    }

    private InterviewQuestionsResponse readQuestionSet(Long meetingId) {
        InterviewQuestionSet questionSet = questionSetRepository.findByMeetingId(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INTERVIEW_NOT_FOUND", "인터뷰 질문을 찾을 수 없습니다."));
        return toResponse(questionSet);
    }

    private InterviewQuestionSet getQuestionSet(Long questionSetId) {
        return questionSetRepository.findById(questionSetId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INTERVIEW_NOT_FOUND", "인터뷰 질문을 찾을 수 없습니다."));
    }

    private InterviewQuestionsResponse toResponse(InterviewQuestionSet questionSet) {
        List<InterviewQuestionsResponse.QuestionResponse> questions = questionRepository
            .findAllByQuestionSetIdOrderByQuestionOrderAsc(questionSet.getId()).stream()
            .map(question -> new InterviewQuestionsResponse.QuestionResponse(
                question.getId(), question.getQuestionOrder(), question.getContent()
            ))
            .toList();
        return new InterviewQuestionsResponse(
            questionSet.getMeeting().getId(), questionSet.getStatus(), questionSet.getFailureMessage(), questions
        );
    }

    private Meeting getMeeting(Long meetingId) {
        return meetingRepository.findWithDetailsById(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "회의를 찾을 수 없습니다."));
    }

    private void requireProjectMember(Meeting meeting, Long userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(meeting.getProject().getId(), userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "회의를 찾을 수 없습니다.");
        }
    }

    /** 트랜잭션 밖으로 넘기는 AI 호출 입력. 엔티티가 아니라 값만 담음. */
    private record PendingGeneration(Long questionSetId, MeetingContext meeting, List<String> agendas) {
    }
}
