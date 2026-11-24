package com.portfolio.hireflow.requisition.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.portfolio.hireflow.requisition.entity.RequisitionStatus;

public record RequisitionResponse(
        Long id,
        Long departmentId,
        String departmentName,
        String jobTitle,
        Integer headcount,
        String reason,
        String requiredSkills,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        LocalDate targetStartDate,
        RequisitionStatus status,
        Long createdById,
        String createdByName,
        String createdByEmail,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        Instant reviewedAt,
        boolean canEdit,
        boolean canSubmit,
        boolean canReview,
        JobOpeningResponse opening,
        List<RequisitionActionResponse> actions
) {
}
