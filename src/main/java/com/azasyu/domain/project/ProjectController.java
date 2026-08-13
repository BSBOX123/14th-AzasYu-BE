package com.azasyu.domain.project;

import com.azasyu.domain.project.dto.CreateProjectRequest;
import com.azasyu.domain.project.dto.JoinProjectRequest;
import com.azasyu.domain.project.dto.ProjectDetailResponse;
import com.azasyu.domain.project.dto.ProjectSummaryResponse;
import com.azasyu.global.api.ApiResponse;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    ResponseEntity<ApiResponse<ProjectDetailResponse>> create(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody CreateProjectRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(projectService.create(userId, request)));
    }

    @PostMapping("/join")
    ApiResponse<ProjectDetailResponse> join(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody JoinProjectRequest request
    ) {
        return ApiResponse.success(projectService.join(userId, request));
    }

    @GetMapping
    ApiResponse<List<ProjectSummaryResponse>> getMyProjects(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(projectService.getMyProjects(userId));
    }

    @GetMapping("/{projectId}")
    ApiResponse<ProjectDetailResponse> getDetail(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long projectId
    ) {
        return ApiResponse.success(projectService.getDetail(userId, projectId));
    }
}
