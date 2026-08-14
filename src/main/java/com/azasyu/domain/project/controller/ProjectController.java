package com.azasyu.domain.project.controller;

import com.azasyu.domain.project.dto.CreateProjectRequest;
import com.azasyu.domain.project.dto.JoinProjectRequest;
import com.azasyu.domain.project.dto.ProjectDetailResponse;
import com.azasyu.domain.project.dto.ProjectSummaryResponse;
import com.azasyu.domain.project.service.ProjectService;
import com.azasyu.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로젝트", description = "프로젝트 생성과 참여 코드 기반 참가")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(
        summary = "프로젝트 생성",
        description = """
            프로젝트를 만들고 생성자를 OWNER로 등록한다.
            8자리 영숫자 참여 코드가 자동 발급되며, 이 코드를 팀원에게 공유해 참가시킨다.

            **성공 시 201 Created**

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 입력 형식이 올바르지 않음 | 400 | `INVALID_REQUEST` |
            """
    )
    @PostMapping
    ResponseEntity<ApiResponse<ProjectDetailResponse>> create(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody CreateProjectRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(projectService.create(userId, request)));
    }

    @Operation(
        summary = "참여 코드로 프로젝트 참가",
        description = """
            8자리 참여 코드로 기존 프로젝트에 MEMBER로 참가한다.
            참가 직후 프로젝트 상세를 그대로 반환하므로 별도 조회가 필요 없다.

            입력한 코드는 공백이 제거되고 대문자로 변환된 뒤 대조하므로
            소문자로 입력해도 참가할 수 있다.

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 존재하지 않는 참여 코드 | 404 | `JOIN_CODE_NOT_FOUND` |
            | 이미 참여 중인 프로젝트 | 409 | `ALREADY_PROJECT_MEMBER` |
            """
    )
    @PostMapping("/join")
    ApiResponse<ProjectDetailResponse> join(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody JoinProjectRequest request
    ) {
        return ApiResponse.success(projectService.join(userId, request));
    }

    @Operation(
        summary = "내 프로젝트 목록",
        description = """
            로그인한 사용자가 참여 중인 프로젝트를 **최근 참여한 순**으로 반환한다.
            참여한 프로젝트가 없으면 빈 배열을 반환한다.

            각 항목에 구성원 목록(`members`)과 프로젝트 생성 시각(`createdAt`)이 포함된다.
            **참여 코드를 제외하면 상세 조회와 같은 정보이므로 목록 화면에서 상세를 다시 호출할 필요가 없다.**

            `joinedAt`은 로그인한 사용자가 참여한 시각이고,
            `members[].joinedAt`은 각 구성원이 참여한 시각이다.
            """
    )
    @GetMapping
    ApiResponse<List<ProjectSummaryResponse>> getMyProjects(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(projectService.getMyProjects(userId));
    }

    @Operation(
        summary = "프로젝트 상세 조회",
        description = """
            참여 코드와 구성원 목록을 포함한 상세 정보를 반환한다.

            구성원이 아닌 사용자가 조회하면 존재 여부를 노출하지 않기 위해
            403이 아니라 404를 반환한다.

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 프로젝트가 없거나 구성원이 아님 | 404 | `PROJECT_NOT_FOUND` |
            """
    )
    @GetMapping("/{projectId}")
    ApiResponse<ProjectDetailResponse> getDetail(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long projectId
    ) {
        return ApiResponse.success(projectService.getDetail(userId, projectId));
    }
}
