package com.portfolio.hireflow.interview.dto;

import java.time.Instant;
import java.util.List;

import com.portfolio.hireflow.interview.entity.InterviewFormat;
import com.portfolio.hireflow.interview.entity.InterviewStatus;

public record InterviewResponse(
        Long id,
        Long applicationId,
        Long candidateId,
        String candidateName,
        String candidateEmail,
        Long jobOpeningId,
        String jobTitle,
        Long departmentId,
        String departmentName,
        int roundNumber,
        Instant startsAt,
        Instant endsAt,
        InterviewFormat format,
        String location,
        InterviewStatus status,
        String cancellationReason,
        List<InterviewerResponse> interviewers,
        List<InterviewScorecardResponse> scorecards,
        boolean canScore,
        boolean canManage,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
