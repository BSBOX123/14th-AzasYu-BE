package com.azasyu.domain.interview;

import com.azasyu.domain.interview.ai.InterviewQuestionAiClient;
import com.azasyu.domain.interview.dto.InterviewQuestionsResponse;
import com.azasyu.domain.meeting.Meeting;
import com.azasyu.domain.meeting.MeetingAgendaRepository;
import com.azasyu.domain.meeting.MeetingParticipantRepository;
import com.azasyu.domain.meeting.MeetingRepository;
import com.azasyu.domain.project.ProjectMemberRepository;
import com.azasyu.global.error.ApiException;
import com.azasyu.global.ai.GeminiApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Transactional
    public InterviewQuestionsResponse initializeAndGenerate(Meeting meeting) {
        InterviewQuestionSet questionSet = questionSetRepository.save(new InterviewQuestionSet(meeting));
        return generate(questionSet);
    }

    @Transactional
    public InterviewQuestionsResponse retry(Long userId, Long meetingId) {
        Meeting meeting = getMeeting(meetingId);
        requireProjectMember(meeting, userId);
        InterviewQuestionSet questionSet = questionSetRepository.findByMeetingId(meetingId)
            .orElseGet(() -> questionSetRepository.save(new InterviewQuestionSet(meeting)));
        if (QuestionGenerationStatus.GENERATED.name().equals(questionSet.getStatus())) {
            return toResponse(questionSet);
        }
        questionSet.pending();
        return generate(questionSet);
    }

    @Transactional(readOnly = true)
    public InterviewQuestionsResponse getQuestions(Long userId, Long meetingId) {
        if (!participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "INTERVIEW_NOT_FOUND", "참여 가능한 인터뷰를 찾을 수 없습니다.");
        }
        InterviewQuestionSet questionSet = questionSetRepository.findByMeetingId(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INTERVIEW_NOT_FOUND", "인터뷰 질문을 찾을 수 없습니다."));
        return toResponse(questionSet);
    }

    private InterviewQuestionsResponse generate(InterviewQuestionSet questionSet) {
        if (!aiClient.isConfigured()) {
            questionSet.notConfigured();
            return toResponse(questionSet);
        }

        try {
            List<String> agendas = agendaRepository
                .findAllByMeetingIdOrderByAgendaOrderAsc(questionSet.getMeeting().getId()).stream()
                .map(agenda -> agenda.getContent())
                .toList();
            List<String> generatedQuestions = aiClient.generate(questionSet.getMeeting(), agendas);
            questionRepository.deleteAllByQuestionSetId(questionSet.getId());
            for (int index = 0; index < generatedQuestions.size(); index++) {
                questionRepository.save(new InterviewQuestion(
                    questionSet, index + 1, generatedQuestions.get(index).trim()
                ));
            }
            questionSet.generated();
        } catch (GeminiApiException exception) {
            log.warn("Gemini interview question generation failed: status={}, message={}",
                exception.getStatusCode(), exception.getMessage());
            questionSet.failed(exception.userMessage());
        } catch (RuntimeException exception) {
            log.error("Interview question generation failed", exception);
            questionSet.failed("질문 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
        return toResponse(questionSet);
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
}
