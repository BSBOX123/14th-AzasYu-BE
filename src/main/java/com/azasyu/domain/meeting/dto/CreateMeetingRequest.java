package com.azasyu.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "회의 생성 요청. 안건과 참여자를 함께 등록한다.")
public record CreateMeetingRequest(
    @Schema(description = "회의 제목 (최대 150자)", example = "MVP 기능 범위 확정")
    @NotBlank @Size(max = 150) String title,

    @Schema(description = "회의 목적 (최대 1000자). AI 질문·분석 생성의 입력으로도 쓰인다.",
        example = "해커톤에서 구현할 기능 범위를 확정한다.")
    @NotBlank @Size(max = 1000) String purpose,

    @Schema(description = "안건 목록. 최소 1개 필요하며 보낸 순서대로 번호가 매겨진다. 각 500자 이하.",
        example = "[\"핵심 기능 범위\", \"발표 시나리오\"]")
    @NotEmpty List<@NotBlank @Size(max = 500) String> agendas,

    @Schema(description = "회의 날짜. 오늘 이후여야 한다.", example = "2026-09-01")
    @NotNull @FutureOrPresent LocalDate meetingDate,

    @Schema(description = "시작 시각", example = "14:00:00")
    @NotNull LocalTime startTime,

    @Schema(description = "예상 소요 시간(분). 1 이상.", example = "60")
    @NotNull @Positive Integer expectedDurationMinutes,

    @Schema(description = """
        회의를 만들면서 함께 등록할 참여자 목록. **생략하거나 빈 배열로 보낼 수 있다.**

        생성자는 지정하지 않아도 항상 참여자로 등록되므로, 혼자 회의를 만든 뒤
        나머지 인원을 나중에 합류시키는 방식으로 써도 된다.

        지정하는 경우 **모두 해당 프로젝트의 구성원**이어야 한다. 중복과 생성자 중복은 자동으로 제거된다.
        """, example = "[1, 2, 3]")
    List<@NotNull Long> participantUserIds
) {
}
