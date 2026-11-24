package com.portfolio.hireflow.decision.dto;

import java.util.List;

import com.portfolio.hireflow.candidate.entity.ApplicationStage;

public record DecisionViewResponse(
        Long applicationId,
        Long candidateId,
        String candidateName,
        String candidateEmail,
        Long jobOpeningId,
        String jobTitle,
        Long departmentId,
        String departmentName,
        ApplicationStage stage,
        long applicationVersion,
        boolean readyForDecision,
        boolean canDecide,
        List<DecisionScorecardResponse> scorecards,
        HiringDecisionResponse decision
) {
}
