package com.portfolio.hireflow.interview.controller;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.portfolio.hireflow.common.response.ApiResponse;
import com.portfolio.hireflow.common.response.PageResponse;
import com.portfolio.hireflow.interview.dto.*;
import com.portfolio.hireflow.interview.entity.InterviewStatus;
import com.portfolio.hireflow.interview.service.InterviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/interviews")
@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'HIRING_MANAGER', 'INTERVIEWER')")
public class InterviewController {

    private final InterviewService service;

    public InterviewController(InterviewService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<InterviewResponse>> findAll(
            @RequestParam(required = false) InterviewStatus status,
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable,
            Authentication authentication
    ) {
        return ApiResponse.ok("已取得面試", service.findAll(
                authentication.getName(), status, applicationId, from, to, pageable));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<InterviewOptionsResponse> getOptions(Authentication authentication) {
        return ApiResponse.ok("已取得面試安排選項", service.getOptions(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ApiResponse<InterviewResponse> findById(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("已取得面試明細", service.findById(id, authentication.getName()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<InterviewResponse>> schedule(
            @Valid @RequestBody ScheduleInterviewRequest request,
            Authentication authentication
    ) {
        InterviewResponse response = service.schedule(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("面試已安排", response));
    }

    @PutMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<InterviewResponse> reschedule(
            @PathVariable Long id, @Valid @RequestBody RescheduleInterviewRequest request,
            Authentication authentication) {
        return ApiResponse.ok("面試已改期，同步工作已排入佇列",
                service.reschedule(id, authentication.getName(), request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<InterviewResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelInterviewRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok("面試已取消", service.cancel(id, authentication.getName(), request));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<InterviewResponse> complete(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("面試已完成", service.complete(id, authentication.getName()));
    }

    @PostMapping("/{id}/scorecards")
    public ApiResponse<InterviewScorecardResponse> saveScorecard(
            @PathVariable Long id,
            @Valid @RequestBody ScorecardRequest request,
            Authentication authentication
    ) {
        String message = request.submit() ? "評分已送出並鎖定" : "評分草稿已儲存";
        return ApiResponse.ok(message, service.saveScorecard(id, authentication.getName(), request));
    }

    @PostMapping("/{interviewId}/scorecards/{scorecardId}/unlock")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<InterviewScorecardResponse> unlockScorecard(
            @PathVariable Long interviewId,
            @PathVariable Long scorecardId,
            @Valid @RequestBody UnlockScorecardRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok("評分已解鎖", service.unlockScorecard(
                interviewId, scorecardId, authentication.getName(), request));
    }
}
