package com.portfolio.hireflow.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelInterviewRequest(
        @NotBlank(message = "取消原因為必填") @Size(max = 1000) String reason
) {
}
