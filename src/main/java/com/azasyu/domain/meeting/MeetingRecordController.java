package com.azasyu.domain.meeting;

import com.azasyu.domain.meeting.dto.CreateMeetingRecordRequest;
import com.azasyu.domain.meeting.dto.MeetingRecordResponse;
import com.azasyu.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "회의 원문", description = "회의 내용 등록. 등록과 동시에 AI 분석이 시작된다.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/record")
public class MeetingRecordController {

    private final MeetingRecordService recordService;

    @Operation(
        summary = "회의 원문 등록 (텍스트 직접 입력)",
        description = """
            회의 내용을 텍스트로 등록한다. 등록과 동시에 AI 회의 분석이 시작된다.

            분석 결과는 이 응답에 포함되지 않는다.
            등록 후 `GET /meetings/{id}/result`로 상태와 결과를 확인한다.

            **회의당 한 번만 등록할 수 있다.** 이미 등록돼 있으면 409가 반환되며,
            수정이나 삭제 API는 제공하지 않는다.
            내용은 최대 50만 자까지 허용한다.

            **성공 시 201 Created**

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 내용이 비어 있음 | 400 | `EMPTY_MEETING_CONTENT` |
            | 회의 참여자가 아니거나 회의 없음 | 404 | `MEETING_NOT_FOUND` |
            | 이미 원문이 등록됨 | 409 | `MEETING_RECORD_ALREADY_EXISTS` |
            | 50만 자 초과 | 413 | `MEETING_CONTENT_TOO_LARGE` |
            """
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<MeetingRecordResponse>> createFromText(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId,
        @Valid @RequestBody CreateMeetingRecordRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(recordService.createFromText(userId, meetingId, request.content())));
    }

    @Operation(
        summary = "회의 원문 등록 (문서 업로드)",
        description = """
            TXT, DOCX, PDF 파일에서 텍스트를 추출해 등록한다.
            추출된 텍스트가 그대로 원문이 되며, 이후 동작은 텍스트 등록과 같다.

            `multipart/form-data`로 `file` 파트에 담아 전송한다.
            **파일 크기는 10MB까지** 허용한다(서버 설정 `max-file-size`).

            **성공 시 201 Created**

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 파일이 비어 있음 | 400 | `EMPTY_DOCUMENT` |
            | 지원하지 않는 형식 | 400 | `UNSUPPORTED_DOCUMENT_TYPE` |
            | 문서를 읽을 수 없음(손상·암호화 등) | 400 | `DOCUMENT_READ_FAILED` |
            | 추출된 텍스트가 비어 있음 | 400 | `EMPTY_MEETING_CONTENT` |
            | 회의 참여자가 아니거나 회의 없음 | 404 | `MEETING_NOT_FOUND` |
            | 이미 원문이 등록됨 | 409 | `MEETING_RECORD_ALREADY_EXISTS` |
            | 추출 결과가 50만 자 초과 | 413 | `MEETING_CONTENT_TOO_LARGE` |
            """
    )
    @PostMapping(path = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<MeetingRecordResponse>> createFromFile(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId,
        @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(recordService.createFromFile(userId, meetingId, file)));
    }

    @Operation(
        summary = "회의 원문 조회",
        description = """
            등록된 회의 원문 전체를 반환한다. 텍스트 입력과 파일 업로드를
            `sourceType`으로 구분하며, 파일이면 `originalFileName`이 채워진다.

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 회의 참여자가 아니거나 회의 없음 | 404 | `MEETING_NOT_FOUND` |
            | 등록된 원문이 없음 | 404 | `MEETING_RECORD_NOT_FOUND` |
            """
    )
    @GetMapping
    ApiResponse<MeetingRecordResponse> get(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId
    ) {
        return ApiResponse.success(recordService.get(userId, meetingId));
    }
}
