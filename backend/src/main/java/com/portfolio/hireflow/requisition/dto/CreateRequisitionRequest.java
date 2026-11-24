package com.portfolio.hireflow.requisition.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRequisitionRequest(
        @NotNull(message = "部門為必填")
        Long departmentId,

        @Size(max = 120, message = "職稱不可超過 120 字")
        String jobTitle,

        @Min(value = 1, message = "需求人數至少為 1")
        Integer headcount,

        @Size(max = 1000, message = "需求原因不可超過 1000 字")
        String reason,

        @Size(max = 2000, message = "技能需求不可超過 2000 字")
        String requiredSkills,

        @DecimalMin(value = "0", message = "最低薪資不可小於 0")
        BigDecimal salaryMin,

        @DecimalMin(value = "0", message = "最高薪資不可小於 0")
        BigDecimal salaryMax,

        LocalDate targetStartDate
) {
}
