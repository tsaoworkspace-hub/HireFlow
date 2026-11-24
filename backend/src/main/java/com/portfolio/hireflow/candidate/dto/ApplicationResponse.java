package com.portfolio.hireflow.candidate.dto;
import java.time.Instant; import java.util.List; import com.portfolio.hireflow.candidate.entity.ApplicationStage;
public record ApplicationResponse(Long id,Long candidateId,String candidateName,String candidateEmail,
 Long jobOpeningId,String jobTitle,Long departmentId,String departmentName,ApplicationStage stage,long version,
 Instant createdAt,Instant updatedAt,List<ApplicationHistoryResponse> histories) {}
