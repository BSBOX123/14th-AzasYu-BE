package com.azasyu.domain.meeting.service;

import com.azasyu.domain.meeting.ai.MeetingAnalysisAiClient;
import com.azasyu.domain.meeting.ai.MeetingAnalysisDraft;
import com.azasyu.domain.meeting.ai.MeetingContext;
import com.azasyu.domain.meeting.dto.MeetingAnalysisResponse;
import com.azasyu.domain.meeting.entity.AmbiguityFinding;
import com.azasyu.domain.meeting.entity.MeetingAnalysis;
import com.azasyu.domain.meeting.entity.MeetingRecord;
import com.azasyu.domain.meeting.repository.AmbiguityFindingRepository;
import com.azasyu.domain.meeting.repository.MeetingAnalysisRepository;
import com.azasyu.domain.meeting.repository.MeetingParticipantRepository;
import com.azasyu.domain.meeting.repository.MeetingRecordRepository;
import com.azasyu.global.error.ApiException;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class MeetingAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(MeetingAnalysisService.class);

    private final MeetingAnalysisRepository analysisRepository;
    private final AmbiguityFindingRepository findingRepository;
    private final MeetingRecordRepository recordRepository;
    private final MeetingParticipantRepository participantRepository;
    private final MeetingAnalysisAiClient aiClient;
    private final TransactionTemplate transactionTemplate;

    /**
     * 회의 원문이 등록된 직후 분석을 시작한다.
     *
     * <p>메서드에 {@code @Transactional}을 걸지 않는다. AI 호출이 트랜잭션 안에서
     * 일어나면 응답이 늦어질 때 DB 커넥션이 그만큼 오래 점유되기 때문이다.
     */
    public MeetingAnalysisResponse initializeAndGenerate(Long meetingId) {
        PendingAnalysis pending = transactionTemplate.execute(status -> {
            MeetingRecord record = getRecord(meetingId);
            MeetingAnalysis analysis = analysisRepository.save(new MeetingAnalysis(record.getMeeting()));
            return toPendingAnalysis(analysis, record);
        });
        return generateAndSave(pending);
    }

    public MeetingAnalysisResponse retry(Long userId, Long meetingId) {
        PendingAnalysis pending = transactionTemplate.execute(status -> {
            requireParticipant(meetingId, userId);
            MeetingRecord record = getRecord(meetingId);
            MeetingAnalysis analysis = analysisRepository.findByMeetingId(meetingId)
                .orElseGet(() -> analysisRepository.save(new MeetingAnalysis(record.getMeeting())));
            analysis.pending();
            return toPendingAnalysis(analysis, record);
        });
        return generateAndSave(pending);
    }

    @Transactional(readOnly = true)
    public MeetingAnalysisResponse get(Long userId, Long meetingId) {
        requireParticipant(meetingId, userId);
        MeetingAnalysis analysis = analysisRepository.findByMeetingId(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEETING_ANALYSIS_NOT_FOUND", "회의 분석 결과가 없습니다."));
        return toResponse(analysis);
    }

    private MeetingAnalysisResponse generateAndSave(PendingAnalysis pending) {
        if (!aiClient.isConfigured()) {
            return updateAnalysis(pending.analysisId(), MeetingAnalysis::notConfigured);
        }

        MeetingAnalysisDraft draft;
        try {
            // 트랜잭션 밖에서 호출한다. 응답이 지연돼도 DB 커넥션을 잡지 않는다.
            draft = aiClient.analyze(pending.meeting(), pending.recordContent());
        } catch (RuntimeException exception) {
            log.warn("Meeting analysis generation failed: meetingId={}", pending.meetingId(), exception);
            return updateAnalysis(pending.analysisId(),
                analysis -> analysis.failed("회의 분석에 실패했습니다. 잠시 후 다시 시도해 주세요."));
        }

        try {
            return transactionTemplate.execute(status -> {
                MeetingAnalysis analysis = getAnalysis(pending.analysisId());
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
                return toResponse(analysis);
            });
        } catch (RuntimeException exception) {
            // 저장 단계가 실패하면 상태가 PENDING에 멈춘다. FAILED로 내려 재시도할 수 있게 한다.
            log.error("Meeting analysis result save failed: analysisId={}", pending.analysisId(), exception);
            return updateAnalysis(pending.analysisId(),
                analysis -> analysis.failed("회의 분석 결과 저장에 실패했습니다. 잠시 후 다시 시도해 주세요."));
        }
    }

    private MeetingAnalysisResponse updateAnalysis(Long analysisId, Consumer<MeetingAnalysis> change) {
        return transactionTemplate.execute(status -> {
            MeetingAnalysis analysis = getAnalysis(analysisId);
            change.accept(analysis);
            return toResponse(analysis);
        });
    }

    private PendingAnalysis toPendingAnalysis(MeetingAnalysis analysis, MeetingRecord record) {
        return new PendingAnalysis(
            analysis.getId(), record.getMeeting().getId(),
            MeetingContext.from(record.getMeeting()), record.getContent()
        );
    }

    private MeetingRecord getRecord(Long meetingId) {
        return recordRepository.findByMeetingId(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "MEETING_RECORD_REQUIRED", "먼저 회의 내용을 등록해 주세요."));
    }

    private MeetingAnalysis getAnalysis(Long analysisId) {
        return analysisRepository.findById(analysisId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEETING_ANALYSIS_NOT_FOUND", "회의 분석 결과가 없습니다."));
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

    /** 트랜잭션 밖으로 넘기는 AI 호출 입력. 엔티티가 아니라 값만 담는다. */
    private record PendingAnalysis(Long analysisId, Long meetingId, MeetingContext meeting, String recordContent) {
    }
}
