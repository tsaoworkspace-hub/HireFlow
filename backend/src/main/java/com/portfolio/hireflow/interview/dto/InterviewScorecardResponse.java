package com.portfolio.hireflow.interview.dto;

import java.time.Instant;

import com.portfolio.hireflow.interview.entity.ScorecardRecommendation;
import com.portfolio.hireflow.interview.entity.ScorecardStatus;

public record InterviewScorecardResponse(
        Long id,
        Long interviewerId,
        String interviewerName,
        int technicalScore,
        int problemAnalysisScore,
        int communicationScore,
        int learningAbilityScore,
        String overallComment,
        ScorecardRecommendation recommendation,
        ScorecardStatus status,
        Instant submittedAt,
        Instant unlockedAt,
        String unlockedByName,
        String unlockReason,
        long version
) {
}
