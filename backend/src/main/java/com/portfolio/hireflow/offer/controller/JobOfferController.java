package com.portfolio.hireflow.offer.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.portfolio.hireflow.common.response.ApiResponse;
import com.portfolio.hireflow.common.response.PageResponse;
import com.portfolio.hireflow.offer.dto.*;
import com.portfolio.hireflow.offer.service.JobOfferService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/offers")
@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'HIRING_MANAGER')")
public class JobOfferController {
    private final JobOfferService service;

    public JobOfferController(JobOfferService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageResponse<JobOfferResponse>> findAll(Authentication authentication, Pageable pageable) {
        return ApiResponse.ok("已取得 Offer 清單", service.findAll(authentication.getName(), pageable));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<List<OfferOptionResponse>> options(Authentication authentication) {
        return ApiResponse.ok("已取得可建立 Offer 的錄取人選", service.options(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobOfferResponse> find(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("已取得 Offer", service.find(id, authentication.getName()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<JobOfferResponse> create(@Valid @RequestBody CreateJobOfferRequest request,
            Authentication authentication) {
        return ApiResponse.ok("Offer 草稿已建立", service.create(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<JobOfferResponse> update(@PathVariable Long id,
            @Valid @RequestBody UpdateJobOfferRequest request, Authentication authentication) {
        return ApiResponse.ok("Offer 草稿已更新", service.update(id, authentication.getName(), request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<JobOfferResponse> submit(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("Offer 已送主管核准", service.submit(id, authentication.getName()));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'HIRING_MANAGER')")
    public ApiResponse<JobOfferResponse> review(@PathVariable Long id,
            @Valid @RequestBody ReviewJobOfferRequest request, Authentication authentication) {
        return ApiResponse.ok("Offer 審核已完成", service.review(id, authentication.getName(), request));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<JobOfferResponse> send(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("Offer 已加入寄信佇列", service.send(id, authentication.getName()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<JobOfferResponse> cancel(@PathVariable Long id,
            @RequestBody(required = false) OfferActionRequest request, Authentication authentication) {
        return ApiResponse.ok("Offer 已取消", service.cancel(id, authentication.getName(),
                request == null ? null : request.reason()));
    }
}
