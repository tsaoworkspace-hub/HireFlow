package com.portfolio.hireflow.audit.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.hireflow.audit.dto.AuditLogResponse;
import com.portfolio.hireflow.audit.entity.AuditAction;
import com.portfolio.hireflow.audit.entity.AuditTargetType;
import com.portfolio.hireflow.audit.service.AuditLogService;
import com.portfolio.hireflow.common.response.ApiResponse;
import com.portfolio.hireflow.common.response.PageResponse;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditLogResponse>> findAll(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditTargetType targetType,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        return ApiResponse.ok("已取得稽核紀錄", service.findAll(
                authentication.getName(), action, targetType, pageable));
    }
}
