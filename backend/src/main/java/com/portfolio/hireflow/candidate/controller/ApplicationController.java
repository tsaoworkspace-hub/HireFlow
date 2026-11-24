package com.portfolio.hireflow.candidate.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.portfolio.hireflow.candidate.dto.ApplicationResponse;
import com.portfolio.hireflow.candidate.dto.CreateApplicationRequest;
import com.portfolio.hireflow.candidate.dto.TransitionApplicationRequest;
import com.portfolio.hireflow.candidate.entity.ApplicationStage;
import com.portfolio.hireflow.candidate.service.CandidateApplicationService;
import com.portfolio.hireflow.common.response.ApiResponse;
import com.portfolio.hireflow.common.response.PageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/applications")
@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'HIRING_MANAGER')")
public class ApplicationController {

    private final CandidateApplicationService service;

    public ApplicationController(CandidateApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<ApplicationResponse>> findAll(
            @RequestParam(required = false) ApplicationStage stage,
            @RequestParam(required = false) Long jobOpeningId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 100, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        return ApiResponse.ok("已取得應徵紀錄", service.findApplications(
                authentication.getName(), stage, jobOpeningId, keyword, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApplicationResponse> findById(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("已取得應徵明細", service.findApplication(id, authentication.getName()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> create(
            @Valid @RequestBody CreateApplicationRequest request,
            Authentication authentication
    ) {
        ApplicationResponse response = service.createApplication(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("應徵紀錄已建立", response));
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<ApplicationResponse> transition(
            @PathVariable Long id,
            @Valid @RequestBody TransitionApplicationRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok("應徵階段已更新", service.transition(id, authentication.getName(), request));
    }
}
