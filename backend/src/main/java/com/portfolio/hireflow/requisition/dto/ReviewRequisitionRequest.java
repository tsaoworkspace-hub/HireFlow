package com.portfolio.hireflow.requisition.dto;

import com.portfolio.hireflow.requisition.entity.ReviewDecision;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequisitionRequest(
        @NotNull(message = "審核結果為必填")
        ReviewDecision decision,

        @Size(max = 1000, message = "審核理由不可超過 1000 字")
        String reason
) {
}
