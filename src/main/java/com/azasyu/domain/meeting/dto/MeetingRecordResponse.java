package com.azasyu.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "등록된 회의 원문")
public record MeetingRecordResponse(
    @Schema(description = "원문 식별자", example = "1")
    Long id,

    @Schema(description = "회의 식별자", example = "1")
    Long meetingId,

    @Schema(description = "원문을 등록한 방식", example = "TEXT",
        allowableValues = {"TEXT", "TXT", "DOCX", "PDF"})
    String sourceType,

    @Schema(description = "업로드한 파일 이름. 텍스트로 직접 입력한 경우 null.",
        example = "회의록.docx")
    String originalFileName,

    @Schema(description = "회의 내용 전문. 파일로 등록한 경우 추출된 텍스트가 담긴다.")
    String content,

    @Schema(description = "등록 시각", example = "2026-08-14T15:00:00")
    LocalDateTime createdAt
) {
}
