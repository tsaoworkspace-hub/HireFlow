package com.portfolio.hireflow.offer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.portfolio.hireflow.offer.entity.JobOfferStatus;

public record PublicJobOfferResponse(
        Long id,
        String companyName,
        String candidateName,
        String jobTitle,
        JobOfferStatus status,
        BigDecimal annualSalary,
        String currency,
        LocalDate proposedStartDate,
        Instant expiresAt,
        String title,
        String message,
        Instant sentAt,
        Instant respondedAt
) {}
