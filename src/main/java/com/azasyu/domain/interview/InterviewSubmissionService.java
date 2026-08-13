package com.azasyu.domain.interview;

import com.azasyu.domain.interview.ai.IdeaCardAiClient;
import com.azasyu.domain.interview.ai.IdeaCardDraft;
import com.azasyu.domain.interview.dto.InterviewSubmissionResponse;
import com.azasyu.domain.interview.dto.SubmitInterviewRequest;
import com.azasyu.domain.meeting.Meeting;
import com.azasyu.domain.meeting.MeetingParticipantRepository;
import com.azasyu.domain.meeting.MeetingRepository;
import com.azasyu.domain.user.User;
import com.azasyu.domain.user.UserRepository;
import com.azasyu.global.error.ApiException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewSubmissionService {

    private final InterviewSubmissionRepository submissionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final InterviewQuestionSetRepository questionSetRepository;
    private final InterviewQuestionRepository questionRepository;
    private final IdeaCardRepository ideaCardRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final IdeaCardAiClient ideaCardAiClient;

    @Transactional
    public InterviewSubmissionResponse submit(Long userId, Long meetingId, SubmitInterviewRequest request) {
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
        generateCard(submission);
        return toResponse(submission);
    }

    @Transactional(readOnly = true)
    public InterviewSubmissionResponse getMine(Long userId, Long meetingId) {
        requireParticipant(meetingId, userId);
        InterviewSubmission submission = submissionRepository.findByMeetingIdAndUserId(meetingId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUBMISSION_NOT_FOUND", "제출한 인터뷰가 없습니다."));
        return toResponse(submission);
    }

    @Transactional
    public InterviewSubmissionResponse retryCardGeneration(Long userId, Long meetingId) {
        requireParticipant(meetingId, userId);
        InterviewSubmission submission = submissionRepository.findByMeetingIdAndUserId(meetingId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUBMISSION_NOT_FOUND", "제출한 인터뷰가 없습니다."));
        if (QuestionGenerationStatus.GENERATED.name().equals(submission.getCardGenerationStatus())) {
            return toResponse(submission);
        }
        submission.pending();
        generateCard(submission);
        return toResponse(submission);
    }

    private void generateCard(InterviewSubmission submission) {
        if (!ideaCardAiClient.isConfigured()) {
            submission.notConfigured();
            return;
        }
        try {
            IdeaCardDraft draft = ideaCardAiClient.generate(
                submission.getMeeting(), answerRepository.findAllBySubmissionIdOrderByQuestionQuestionOrderAsc(submission.getId())
            );
            ideaCardRepository.save(new IdeaCard(
                submission, draft.coreOpinion(), draft.rationale(), draft.concern(), draft.alternative()
            ));
            submission.generated();
        } catch (RuntimeException exception) {
            submission.failed("아이디어 카드 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
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
}
