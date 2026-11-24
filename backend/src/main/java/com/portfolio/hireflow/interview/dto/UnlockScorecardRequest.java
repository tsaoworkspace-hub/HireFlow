package com.portfolio.hireflow.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnlockScorecardRequest(
        @NotBlank(message = "解鎖原因為必填") @Size(max = 1000) String reason
) {
}
