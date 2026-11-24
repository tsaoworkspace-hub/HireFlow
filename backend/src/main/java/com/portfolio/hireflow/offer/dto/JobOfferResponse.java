package com.portfolio.hireflow.offer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.portfolio.hireflow.offer.entity.JobOfferStatus;

public record JobOfferResponse(
        Long id,
        Long applicationId,
        Long candidateId,
        String candidateName,
        String candidateEmail,
        String jobTitle,
        Long departmentId,
        String departmentName,
        JobOfferStatus status,
        BigDecimal annualSalary,
        String currency,
        LocalDate proposedStartDate,
        Instant expiresAt,
        String title,
        String message,
        String createdByName,
        String approvedByName,
        Instant sentAt,
        Instant respondedAt,
        String responseReason,
        long version,
        Instant createdAt,
        Instant updatedAt,
        boolean canEdit,
        boolean canSubmit,
        boolean canReview,
        boolean canSend,
        boolean canCancel,
        List<JobOfferHistoryResponse> histories
) {}
