package com.azasyu.domain.meeting;

import com.azasyu.domain.meeting.MeetingDocumentTextExtractor.ExtractedDocument;
import com.azasyu.domain.meeting.dto.MeetingRecordResponse;
import com.azasyu.domain.user.User;
import com.azasyu.domain.user.UserRepository;
import com.azasyu.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MeetingRecordService {

    private static final int MAX_CONTENT_LENGTH = 500_000;
    private final MeetingRecordRepository recordRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final MeetingDocumentTextExtractor textExtractor;
    private final MeetingAnalysisService analysisService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 회의 원문을 저장하고 분석 생성을 시작한다.
     *
     * <p>메서드에 {@code @Transactional}을 걸지 않는다. 원문 저장을 먼저 커밋한 뒤
     * 분석을 시작해야 AI 호출이 트랜잭션 밖에서 이루어진다.
     */
    public MeetingRecordResponse createFromText(Long userId, Long meetingId, String content) {
        return createAndAnalyze(userId, meetingId, MeetingRecordSourceType.TEXT, null, content);
    }

    public MeetingRecordResponse createFromFile(Long userId, Long meetingId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_DOCUMENT", "업로드할 파일이 비어 있습니다.");
        }
        // 문서 텍스트 추출도 트랜잭션 밖에서 처리한다.
        ExtractedDocument document = textExtractor.extract(file);
        return createAndAnalyze(userId, meetingId, document.sourceType(), document.fileName(), document.content());
    }

    private MeetingRecordResponse createAndAnalyze(
        Long userId, Long meetingId, MeetingRecordSourceType type, String fileName, String rawContent
    ) {
        MeetingRecordResponse response = transactionTemplate.execute(
            status -> create(userId, meetingId, type, fileName, rawContent)
        );
        analysisService.initializeAndGenerate(meetingId);
        return response;
    }

    @Transactional(readOnly = true)
    public MeetingRecordResponse get(Long userId, Long meetingId) {
        requireParticipant(meetingId, userId);
        return recordRepository.findByMeetingId(meetingId).map(this::toResponse)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEETING_RECORD_NOT_FOUND", "등록된 회의 내용이 없습니다."));
    }

    private MeetingRecordResponse create(
        Long userId, Long meetingId, MeetingRecordSourceType type, String fileName, String rawContent
    ) {
        requireParticipant(meetingId, userId);
        if (recordRepository.existsByMeetingId(meetingId)) {
            throw new ApiException(HttpStatus.CONFLICT, "MEETING_RECORD_ALREADY_EXISTS", "회의 내용이 이미 등록되어 있습니다.");
        }
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_MEETING_CONTENT", "추출된 회의 내용이 비어 있습니다.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ApiException(HttpStatus.CONTENT_TOO_LARGE, "MEETING_CONTENT_TOO_LARGE", "회의 내용은 50만 자 이하만 등록할 수 있습니다.");
        }
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "회의를 찾을 수 없습니다."));
        User user = userRepository.findById(userId).orElseThrow();
        MeetingRecord record = recordRepository.save(new MeetingRecord(meeting, user, type, fileName, content));
        return toResponse(record);
    }

    private MeetingRecordResponse toResponse(MeetingRecord record) {
        return new MeetingRecordResponse(
            record.getId(), record.getMeeting().getId(), record.getSourceType(), record.getOriginalFileName(),
            record.getContent(), record.getCreatedAt()
        );
    }

    private void requireParticipant(Long meetingId, Long userId) {
        if (!participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "회의를 찾을 수 없습니다.");
        }
    }
}
