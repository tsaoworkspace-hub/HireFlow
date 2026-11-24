package com.portfolio.hireflow.requisition.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.portfolio.hireflow.common.response.ApiResponse;
import com.portfolio.hireflow.common.response.PageResponse;
import com.portfolio.hireflow.requisition.dto.JobOpeningListResponse;
import com.portfolio.hireflow.requisition.entity.JobOpeningStatus;
import com.portfolio.hireflow.requisition.service.JobOpeningService;

@RestController
@RequestMapping("/api/job-openings")
@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'HIRING_MANAGER')")
public class JobOpeningController {

    private final JobOpeningService service;

    public JobOpeningController(JobOpeningService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<JobOpeningListResponse>> findAll(
            @RequestParam(required = false) JobOpeningStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        return ApiResponse.ok("已取得職缺", service.findAll(authentication.getName(), status, keyword, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobOpeningListResponse> findById(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("已取得職缺明細", service.findById(id, authentication.getName()));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<JobOpeningListResponse> close(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("職缺已關閉", service.close(id, authentication.getName()));
    }
}
