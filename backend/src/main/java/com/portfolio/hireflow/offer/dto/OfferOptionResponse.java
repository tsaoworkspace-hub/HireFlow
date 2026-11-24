package com.portfolio.hireflow.offer.dto;

public record OfferOptionResponse(
        Long applicationId,
        String candidateName,
        String candidateEmail,
        String jobTitle,
        String departmentName
) {}
