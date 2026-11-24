package com.portfolio.hireflow.decision.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.portfolio.hireflow.interview.entity.ScorecardRecommendation;

public record DecisionScorecardResponse(
        Long scorecardId,
        Long interviewId,
        int roundNumber,
        Long interviewerId,
        String interviewerName,
        int technicalScore,
        int problemAnalysisScore,
        int communicationScore,
        int learningAbilityScore,
        BigDecimal averageScore,
        String overallComment,
        ScorecardRecommendation recommendation,
        Instant submittedAt
) {
}
