package com.portfolio.hireflow.candidate.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.portfolio.hireflow.candidate.dto.CandidateRequest;
import com.portfolio.hireflow.candidate.dto.CandidateResponse;
import com.portfolio.hireflow.candidate.dto.CandidateResumeResponse;
import com.portfolio.hireflow.candidate.service.CandidateApplicationService;
import com.portfolio.hireflow.common.response.ApiResponse;
import com.portfolio.hireflow.common.response.PageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/candidates")
@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'HIRING_MANAGER')")
public class CandidateController {

    private final CandidateApplicationService service;

    public CandidateController(CandidateApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<CandidateResponse>> findAll(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        return ApiResponse.ok("已取得應徵者", service.findCandidates(authentication.getName(), keyword, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<CandidateResponse>> create(
            @Valid @RequestBody CandidateRequest request,
            Authentication authentication
    ) {
        CandidateResponse response = service.createCandidate(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("應徵者已建立", response));
    }

    @GetMapping("/{id}/resume")
    public ApiResponse<CandidateResumeResponse> resume(
            @PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("已取得求職者履歷", service.findCandidateResume(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<CandidateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CandidateRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok("應徵者已更新", service.updateCandidate(id, authentication.getName(), request));
    }
}
