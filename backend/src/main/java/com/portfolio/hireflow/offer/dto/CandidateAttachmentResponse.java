package com.portfolio.hireflow.offer.dto;

import java.time.Instant;

public record CandidateAttachmentResponse(
        Long id,
        Long applicationId,
        String originalFilename,
        String contentType,
        long fileSize,
        String uploadedByType,
        Instant createdAt
) {}
