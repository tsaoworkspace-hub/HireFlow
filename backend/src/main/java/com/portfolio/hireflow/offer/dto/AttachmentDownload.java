package com.portfolio.hireflow.offer.dto;

import org.springframework.core.io.Resource;

public record AttachmentDownload(
        Resource resource,
        String originalFilename,
        String contentType,
        long fileSize
) {}
