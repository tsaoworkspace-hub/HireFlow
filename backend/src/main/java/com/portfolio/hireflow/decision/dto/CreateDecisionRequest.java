package com.portfolio.hireflow.decision.dto;

import com.portfolio.hireflow.decision.entity.DecisionResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDecisionRequest(
        @NotNull(message = "決策結果為必填") DecisionResult result,
        @NotBlank(message = "決策說明為必填")
        @Size(max = 2000, message = "決策說明不可超過 2000 字") String reason,
        @NotNull(message = "Application 版本為必填") Long applicationVersion
) {
}
