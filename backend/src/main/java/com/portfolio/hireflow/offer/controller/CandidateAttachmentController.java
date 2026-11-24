package com.portfolio.hireflow.offer.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.portfolio.hireflow.common.response.ApiResponse;
import com.portfolio.hireflow.offer.dto.AttachmentDownload;
import com.portfolio.hireflow.offer.dto.CandidateAttachmentResponse;
import com.portfolio.hireflow.offer.service.CandidateAttachmentService;

@RestController
public class CandidateAttachmentController {
    private final CandidateAttachmentService service;

    public CandidateAttachmentController(CandidateAttachmentService service) { this.service = service; }

    @GetMapping("/api/applications/{applicationId}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'HIRING_MANAGER')")
    public ApiResponse<List<CandidateAttachmentResponse>> findForApplication(
            @PathVariable Long applicationId, Authentication authentication) {
        return ApiResponse.ok("已取得履歷附件", service.findForApplication(applicationId, authentication.getName()));
    }

    @GetMapping("/api/attachments/{attachmentId}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'HIRING_MANAGER')")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable Long attachmentId, Authentication authentication) {
        AttachmentDownload download = service.download(attachmentId, authentication.getName());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.originalFilename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }

    @GetMapping("/api/public/applications/{publicId}/resume")
    public ApiResponse<List<CandidateAttachmentResponse>> findPublic(
            @PathVariable UUID publicId, @RequestParam String token) {
        return ApiResponse.ok("已取得履歷附件", service.findPublic(publicId, token));
    }

    @PostMapping(value = "/api/public/applications/{publicId}/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CandidateAttachmentResponse> uploadPublic(
            @PathVariable UUID publicId, @RequestParam String token, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok("PDF 履歷已更新", service.uploadPublic(publicId, token, file));
    }
}
