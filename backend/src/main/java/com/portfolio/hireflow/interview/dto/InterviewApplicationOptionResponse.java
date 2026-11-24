package com.portfolio.hireflow.interview.dto;

import com.portfolio.hireflow.candidate.entity.ApplicationStage;

public record InterviewApplicationOptionResponse(
        Long id,
        String candidateName,
        String jobTitle,
        String departmentName,
        ApplicationStage stage
) {
}
