package com.portfolio.hireflow.requisition.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.hireflow.common.response.ApiResponse;
import com.portfolio.hireflow.common.response.PageResponse;
import com.portfolio.hireflow.requisition.dto.CreateRequisitionRequest;
import com.portfolio.hireflow.requisition.dto.RequisitionOptionsResponse;
import com.portfolio.hireflow.requisition.dto.RequisitionResponse;
import com.portfolio.hireflow.requisition.dto.ReviewRequisitionRequest;
import com.portfolio.hireflow.requisition.dto.UpdateRequisitionRequest;
import com.portfolio.hireflow.requisition.entity.RequisitionStatus;
import com.portfolio.hireflow.requisition.service.RequisitionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/requisitions")
@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'HIRING_MANAGER')")
public class RequisitionController {

    private final RequisitionService requisitionService;

    public RequisitionController(RequisitionService requisitionService) {
        this.requisitionService = requisitionService;
    }

    @GetMapping
    public ApiResponse<PageResponse<RequisitionResponse>> findAll(
            @RequestParam(required = false) RequisitionStatus status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long createdById,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "已取得人力需求",
                requisitionService.findAll(
                        authentication.getName(),
                        status,
                        departmentId,
                        createdById,
                        keyword,
                        pageable
                )
        );
    }

    @GetMapping("/options")
    public ApiResponse<RequisitionOptionsResponse> getOptions(Authentication authentication) {
        return ApiResponse.ok("已取得表單選項", requisitionService.getOptions(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ApiResponse<RequisitionResponse> findById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ApiResponse.ok("已取得人力需求明細", requisitionService.findById(id, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RequisitionResponse>> create(
            @Valid @RequestBody CreateRequisitionRequest request,
            Authentication authentication
    ) {
        RequisitionResponse response = requisitionService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("人力需求草稿已建立", response));
    }

    @PutMapping("/{id}")
    public ApiResponse<RequisitionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequisitionRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok("人力需求已更新", requisitionService.update(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<RequisitionResponse> submit(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ApiResponse.ok("人力需求已送出審核", requisitionService.submit(id, authentication.getName()));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApiResponse<RequisitionResponse> review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequisitionRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok("人力需求審核已完成", requisitionService.review(id, request, authentication.getName()));
    }
}
