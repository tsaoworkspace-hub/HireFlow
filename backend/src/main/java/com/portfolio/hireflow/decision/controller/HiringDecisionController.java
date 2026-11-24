package com.portfolio.hireflow.decision.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.portfolio.hireflow.common.response.ApiResponse;
import com.portfolio.hireflow.decision.dto.CreateDecisionRequest;
import com.portfolio.hireflow.decision.dto.DecisionViewResponse;
import com.portfolio.hireflow.decision.service.HiringDecisionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/applications/{applicationId}")
@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'HIRING_MANAGER')")
public class HiringDecisionController {

    private final HiringDecisionService service;

    public HiringDecisionController(HiringDecisionService service) {
        this.service = service;
    }

    @GetMapping("/decision-view")
    public ApiResponse<DecisionViewResponse> findDecisionView(
            @PathVariable Long applicationId,
            Authentication authentication
    ) {
        return ApiResponse.ok("已取得決策評分彙整", service.findDecisionView(
                applicationId, authentication.getName()));
    }

    @PostMapping("/decision")
    @PreAuthorize("hasAnyRole('ADMIN', 'HIRING_MANAGER')")
    public ApiResponse<DecisionViewResponse> decide(
            @PathVariable Long applicationId,
            @Valid @RequestBody CreateDecisionRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok("錄取決策已建立", service.decide(
                applicationId, authentication.getName(), request));
    }
}
