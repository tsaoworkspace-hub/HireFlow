package com.portfolio.hireflow.decision.dto;

import java.time.Instant;

import com.portfolio.hireflow.decision.entity.DecisionResult;

public record HiringDecisionResponse(
        Long id,
        DecisionResult result,
        String reason,
        Long decidedById,
        String decidedByName,
        Instant decidedAt
) {
}
