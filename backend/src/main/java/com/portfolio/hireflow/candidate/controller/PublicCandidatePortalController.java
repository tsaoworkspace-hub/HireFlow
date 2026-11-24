package com.portfolio.hireflow.candidate.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.portfolio.hireflow.candidate.dto.*;
import com.portfolio.hireflow.candidate.service.PublicCandidatePortalService;
import com.portfolio.hireflow.common.response.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/public")
public class PublicCandidatePortalController {
    private final PublicCandidatePortalService service;
    public PublicCandidatePortalController(PublicCandidatePortalService service) { this.service = service; }

    @GetMapping("/jobs")
    public ApiResponse<List<PublicJobResponse>> jobs() {
        return ApiResponse.ok("已取得公開職缺", service.findPublishedJobs());
    }
    @GetMapping("/jobs/{slug}")
    public ApiResponse<PublicJobResponse> job(@PathVariable String slug) {
        return ApiResponse.ok("已取得公開職缺", service.findPublishedJob(slug));
    }
    @PostMapping("/jobs/{slug}/applications")
    public ResponseEntity<ApiResponse<PublicApplicationCreatedResponse>> apply(
            @PathVariable String slug, @Valid @RequestBody PublicApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("履歷已送出", service.apply(slug, request)));
    }
    @GetMapping("/applications/{publicId}")
    public ApiResponse<CandidatePortalResponse> application(
            @PathVariable UUID publicId, @RequestParam String token) {
        return ApiResponse.ok("已取得應徵進度", service.findApplication(publicId, token));
    }
}
