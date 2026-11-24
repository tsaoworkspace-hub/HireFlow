package com.portfolio.hireflow.requisition.dto;

import java.time.Instant;

import com.portfolio.hireflow.requisition.entity.JobOpeningStatus;

public record JobOpeningResponse(
        Long id,
        JobOpeningStatus status,
        Instant createdAt
) {
}
