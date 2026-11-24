package com.portfolio.hireflow.requisition.dto;

import java.util.List;

public record RequisitionOptionsResponse(
        List<DepartmentOptionResponse> departments,
        List<String> statuses
) {
}
