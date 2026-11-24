package com.portfolio.hireflow.requisition.dto;
import java.time.Instant; import com.portfolio.hireflow.requisition.entity.JobOpeningStatus;
public record JobOpeningListResponse(Long id,Long requisitionId,Long departmentId,String departmentName,String jobTitle,
 int headcount,JobOpeningStatus status,String responsibleHrName,long applicationCount,long version,
 Instant createdAt,Instant closedAt) {}
