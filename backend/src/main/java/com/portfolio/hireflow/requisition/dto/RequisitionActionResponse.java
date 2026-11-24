package com.portfolio.hireflow.requisition.dto;

import java.time.Instant;

import com.portfolio.hireflow.requisition.entity.RequisitionActionType;
import com.portfolio.hireflow.requisition.entity.RequisitionStatus;

public record RequisitionActionResponse(
        Long id,
        RequisitionActionType actionType,
        RequisitionStatus fromStatus,
        RequisitionStatus toStatus,
        String reason,
        Long actorId,
        String actorName,
        Instant createdAt
) {
}
