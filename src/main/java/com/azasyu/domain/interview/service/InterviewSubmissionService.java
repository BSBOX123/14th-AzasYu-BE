package com.azasyu.domain.interview.service;

import com.azasyu.domain.interview.ai.IdeaCardAiClient;
import com.azasyu.domain.interview.ai.IdeaCardDraft;
import com.azasyu.domain.interview.ai.InterviewAnswerContext;
import com.azasyu.domain.interview.dto.InterviewSubmissionResponse;
import com.azasyu.domain.interview.dto.SubmitInterviewRequest;
import com.azasyu.domain.interview.entity.IdeaCard;
import com.azasyu.domain.interview.entity.InterviewAnswer;
import com.azasyu.domain.interview.entity.InterviewQuestion;
import com.azasyu.domain.interview.entity.InterviewQuestionSet;
import com.azasyu.domain.interview.entity.InterviewSubmission;
import com.azasyu.domain.interview.entity.QuestionGenerationStatus;
import com.azasyu.domain.interview.repository.IdeaCardRepository;
import com.azasyu.domain.interview.repository.InterviewAnswerRepository;
import com.azasyu.domain.interview.repository.InterviewQuestionRepository;
import com.azasyu.domain.interview.repository.InterviewQuestionSetRepository;
import com.azasyu.domain.interview.repository.InterviewSubmissionRepository;
import com.azasyu.domain.meeting.ai.MeetingContext;
import com.azasyu.domain.meeting.entity.Meeting;
import com.azasyu.domain.meeting.repository.MeetingAgendaRepository;
import com.azasyu.domain.meeting.repository.MeetingParticipantRepository;
import com.azasyu.domain.meeting.repository.MeetingRepository;
import com.azasyu.domain.user.User;
import com.azasyu.domain.user.UserRepository;
import com.azasyu.global.error.ApiException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 인터뷰 답변 제출과 개인 아이디어 카드 생성.
 *
 * <p>공통 질문 전체에 하나씩 답변해야 하며 회의당 1회만 제출할 수 있음. *
 * <p>AI 호출은 트랜잭션 밖에서 수행함. 트랜잭션 안에서 호출하면 응답이 늦어질 때
 * DB 커넥션이 그만큼 오래 점유됨. 상태 저장은 {@code TransactionTemplate}으로
 * 짧은 트랜잭션을 열어 처리하므로 서비스 메서드에 {@code @Transactional}을 걸지 않음.
 */
@Service
@RequiredArgsConstructor
public class InterviewSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(InterviewSubmissionService.class);

    private final InterviewSubmissionRepository submissionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final InterviewQuestionSetRepository questionSetRepository;
    private final InterviewQuestionRepository questionRepository;
    private final IdeaCardRepository ideaCardRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingAgendaRepository meetingAgendaRepository;
    private final MeetingParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final IdeaCardAiClient ideaCardAiClient;
    private final TransactionTemplate transactionTemplate;

    /**
     * 인터뷰 답변을 저장하고 아이디어 카드 생성을 시작함.
     *
     * <p>답변 저장을 먼저 커밋하므로 카드 생성이 실패해도 제출 자체는 보존됨.
     */
    public InterviewSubmissionResponse submit(Long userId, Long meetingId, SubmitInterviewRequest request) {
        PendingCard pending = transactionTemplate.execute(status -> {
            requireParticipant(meetingId, userId);
            if (submissionRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
                throw new ApiException(HttpStatus.CONFLICT, "INTERVIEW_ALREADY_SUBMITTED", "이미 인터뷰를 제출했습니다.");
            }

            InterviewQuestionSet questionSet = questionSetRepository.findByMeetingId(meetingId)
                .filter(set -> QuestionGenerationStatus.GENERATED.name().equals(set.getStatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "QUESTIONS_NOT_READY", "인터뷰 질문이 아직 준비되지 않았습니다."));
            List<InterviewQuestion> questions = questionRepository
                .findAllByQuestionSetIdOrderByQuestionOrderAsc(questionSet.getId());
            Map<Long, InterviewQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(InterviewQuestion::getId, Function.identity()));
            Map<Long, String> submittedAnswers = new LinkedHashMap<>();
            request.answers().forEach(answer -> {
                if (submittedAnswers.put(answer.questionId(), answer.content()) != null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_ANSWER", "같은 질문에 답변이 중복 제출되었습니다.");
                }
            });
            if (!submittedAnswers.keySet().equals(questionMap.keySet())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INCOMPLETE_ANSWERS", "모든 인터뷰 질문에 한 번씩 답변해야 합니다.");
            }

            Meeting meeting = meetingRepository.findWithDetailsById(meetingId).orElseThrow();
            User user = userRepository.findById(userId).orElseThrow();
            InterviewSubmission submission = submissionRepository.save(new InterviewSubmission(meeting, user));
            questions.forEach(question -> answerRepository.save(
                new InterviewAnswer(submission, question, submittedAnswers.get(question.getId()).trim())
            ));
            return toPendingCard(submission, meeting);
        });
        return generateCardAndSave(pending);
    }

    /**
     * 회의에서 인터뷰를 제출한 인원 수.
     *
     * <p>회의 상세의 참여 현황 표시에 쓰므로 참여자 여부를 검사하지 않음.
     */
    @Transactional(readOnly = true)
    public long countSubmissions(Long meetingId) {
        return submissionRepository.countByMeetingId(meetingId);
    }

    /** 해당 사용자가 이 회의에 인터뷰를 제출했는지. 회의 참여자가 아니면 항상 false. */
    @Transactional(readOnly = true)
    public boolean hasSubmitted(Long meetingId, Long userId) {
        return submissionRepository.existsByMeetingIdAndUserId(meetingId, userId);
    }

    @Transactional(readOnly = true)
    public InterviewSubmissionResponse getMine(Long userId, Long meetingId) {
        requireParticipant(meetingId, userId);
        return toResponse(getSubmission(meetingId, userId));
    }

    public InterviewSubmissionResponse retryCardGeneration(Long userId, Long meetingId) {
        Optional<PendingCard> pending = transactionTemplate.execute(status -> {
            requireParticipant(meetingId, userId);
            InterviewSubmission submission = getSubmission(meetingId, userId);
            if (QuestionGenerationStatus.GENERATED.name().equals(submission.getCardGenerationStatus())) {
                return Optional.empty();
            }
            submission.pending();
            return Optional.of(toPendingCard(submission, submission.getMeeting()));
        });
        return pending
            .map(this::generateCardAndSave)
            .orElseGet(() -> transactionTemplate.execute(status -> toResponse(getSubmission(meetingId, userId))));
    }

    private InterviewSubmissionResponse generateCardAndSave(PendingCard pending) {
        if (!ideaCardAiClient.isConfigured()) {
            return updateSubmission(pending.submissionId(), InterviewSubmission::notConfigured);
        }

        IdeaCardDraft draft;
        try {
            // 트랜잭션 밖에서 호출함. 응답이 지연돼도 DB 커넥션을 잡지 않음.
            draft = ideaCardAiClient.generate(pending.meeting(), pending.agendas(), pending.answers());
        } catch (RuntimeException exception) {
            log.warn("Idea card generation failed: submissionId={}", pending.submissionId(), exception);
            return updateSubmission(pending.submissionId(),
                submission -> submission.failed("아이디어 카드 생성에 실패했습니다. 잠시 후 다시 시도해 주세요."));
        }

        try {
            return transactionTemplate.execute(status -> {
                InterviewSubmission submission = getSubmission(pending.submissionId());
                ideaCardRepository.save(new IdeaCard(
                    submission, draft.coreOpinion(), draft.rationale(), draft.concern(), draft.alternative()
                ));
                submission.generated();
                return toResponse(submission);
            });
        } catch (RuntimeException exception) {
            // 저장 단계가 실패하면 상태가 PENDING에 멈춤. FAILED로 내려 재시도할 수 있게 함.
            log.error("Idea card save failed: submissionId={}", pending.submissionId(), exception);
            return updateSubmission(pending.submissionId(),
                submission -> submission.failed("아이디어 카드 저장에 실패했습니다. 잠시 후 다시 시도해 주세요."));
        }
    }

    private InterviewSubmissionResponse updateSubmission(Long submissionId, Consumer<InterviewSubmission> change) {
        return transactionTemplate.execute(status -> {
            InterviewSubmission submission = getSubmission(submissionId);
            change.accept(submission);
            return toResponse(submission);
        });
    }

    private PendingCard toPendingCard(InterviewSubmission submission, Meeting meeting) {
        List<String> agendas = meetingAgendaRepository
            .findAllByMeetingIdOrderByAgendaOrderAsc(meeting.getId()).stream()
            .map(agenda -> agenda.getContent())
            .toList();
        List<InterviewAnswerContext> answers = answerRepository
            .findAllBySubmissionIdOrderByQuestionQuestionOrderAsc(submission.getId()).stream()
            .map(InterviewAnswerContext::from)
            .toList();
        return new PendingCard(submission.getId(), MeetingContext.from(meeting), agendas, answers);
    }

    private InterviewSubmission getSubmission(Long meetingId, Long userId) {
        return submissionRepository.findByMeetingIdAndUserId(meetingId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUBMISSION_NOT_FOUND", "제출한 인터뷰가 없습니다."));
    }

    private InterviewSubmission getSubmission(Long submissionId) {
        return submissionRepository.findById(submissionId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUBMISSION_NOT_FOUND", "제출한 인터뷰가 없습니다."));
    }

    private InterviewSubmissionResponse toResponse(InterviewSubmission submission) {
        InterviewSubmissionResponse.IdeaCardResponse cardResponse = ideaCardRepository
            .findBySubmissionId(submission.getId())
            .map(card -> new InterviewSubmissionResponse.IdeaCardResponse(
                card.getId(), card.getCoreOpinion(), card.getRationale(), card.getConcern(),
                card.getAlternative(), card.getCreatedAt()
            ))
            .orElse(null);
        return new InterviewSubmissionResponse(
            submission.getId(), submission.getMeeting().getId(), submission.getCardGenerationStatus(),
            submission.getFailureMessage(), submission.getSubmittedAt(), cardResponse
        );
    }

    private void requireParticipant(Long meetingId, Long userId) {
        if (!participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "INTERVIEW_NOT_FOUND", "참여 가능한 인터뷰를 찾을 수 없습니다.");
        }
    }

    /** 트랜잭션 밖으로 넘기는 AI 호출 입력. 엔티티가 아니라 값만 담음. */
    private record PendingCard(
        Long submissionId,
        MeetingContext meeting,
        List<String> agendas,
        List<InterviewAnswerContext> answers
    ) {
    }
}
