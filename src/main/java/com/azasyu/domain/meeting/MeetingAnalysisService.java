package com.azasyu.domain.meeting;

import com.azasyu.domain.meeting.ai.MeetingAnalysisAiClient;
import com.azasyu.domain.meeting.ai.MeetingAnalysisDraft;
import com.azasyu.domain.meeting.dto.MeetingAnalysisResponse;
import com.azasyu.global.error.ApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingAnalysisService {

    private final MeetingAnalysisRepository analysisRepository;
    private final AmbiguityFindingRepository findingRepository;
    private final MeetingRecordRepository recordRepository;
    private final MeetingParticipantRepository participantRepository;
    private final MeetingAnalysisAiClient aiClient;

    @Transactional
    public MeetingAnalysisResponse initializeAndGenerate(MeetingRecord record) {
        MeetingAnalysis analysis = analysisRepository.save(new MeetingAnalysis(record.getMeeting()));
        return generate(analysis, record);
    }

    @Transactional
    public MeetingAnalysisResponse retry(Long userId, Long meetingId) {
        requireParticipant(meetingId, userId);
        MeetingRecord record = recordRepository.findByMeetingId(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "MEETING_RECORD_REQUIRED", "먼저 회의 내용을 등록해 주세요."));
        MeetingAnalysis analysis = analysisRepository.findByMeetingId(meetingId)
            .orElseGet(() -> analysisRepository.save(new MeetingAnalysis(record.getMeeting())));
        analysis.pending();
        return generate(analysis, record);
    }

    @Transactional(readOnly = true)
    public MeetingAnalysisResponse get(Long userId, Long meetingId) {
        requireParticipant(meetingId, userId);
        MeetingAnalysis analysis = analysisRepository.findByMeetingId(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEETING_ANALYSIS_NOT_FOUND", "회의 분석 결과가 없습니다."));
        return toResponse(analysis);
    }

    private MeetingAnalysisResponse generate(MeetingAnalysis analysis, MeetingRecord record) {
        if (!aiClient.isConfigured()) {
            analysis.notConfigured();
            return toResponse(analysis);
        }
        try {
            MeetingAnalysisDraft draft = aiClient.analyze(record.getMeeting(), record.getContent());
            findingRepository.deleteAllByAnalysisId(analysis.getId());
            analysis.generated(
                draft.meetingPurpose(), draft.keyDiscussions(), draft.decisions(), draft.followUpChecks()
            );
            List<MeetingAnalysisDraft.AmbiguityDraft> ambiguities = draft.ambiguities() == null
                ? List.of() : draft.ambiguities();
            for (int index = 0; index < ambiguities.size(); index++) {
                var ambiguity = ambiguities.get(index);
                findingRepository.save(new AmbiguityFinding(
                    analysis, index + 1, ambiguity.expression(), ambiguity.reason()
                ));
            }
        } catch (RuntimeException exception) {
            analysis.failed("회의 분석에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
        return toResponse(analysis);
    }

    private MeetingAnalysisResponse toResponse(MeetingAnalysis analysis) {
        List<MeetingAnalysisResponse.AmbiguityResponse> ambiguities = findingRepository
            .findAllByAnalysisIdOrderByFindingOrderAsc(analysis.getId()).stream()
            .map(finding -> new MeetingAnalysisResponse.AmbiguityResponse(
                finding.getId(), finding.getFindingOrder(), finding.getExpression(), finding.getReason()
            )).toList();
        return new MeetingAnalysisResponse(
            analysis.getId(), analysis.getMeeting().getId(), analysis.getStatus(), analysis.getFailureMessage(),
            analysis.getMeetingPurpose(), analysis.getKeyDiscussions(), analysis.getDecisions(),
            analysis.getFollowUpChecks(), ambiguities, analysis.getUpdatedAt()
        );
    }

    private void requireParticipant(Long meetingId, Long userId) {
        if (!participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "회의를 찾을 수 없습니다.");
        }
    }
}
