package com.portfolio.hireflow.audit.dto;

import java.time.Instant;

import com.portfolio.hireflow.audit.entity.AuditAction;
import com.portfolio.hireflow.audit.entity.AuditOutcome;
import com.portfolio.hireflow.audit.entity.AuditTargetType;

public record AuditLogResponse(
        Long id,
        Long actorId,
        String actorName,
        String actorEmail,
        AuditAction action,
        AuditTargetType targetType,
        Long targetId,
        AuditOutcome outcome,
        String details,
        Instant createdAt
) {
}
