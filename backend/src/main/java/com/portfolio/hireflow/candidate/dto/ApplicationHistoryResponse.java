package com.portfolio.hireflow.candidate.dto;
import java.time.Instant; import com.portfolio.hireflow.candidate.entity.ApplicationStage;
public record ApplicationHistoryResponse(Long id,ApplicationStage fromStage,ApplicationStage toStage,String reason,
 Long actorId,String actorName,Instant createdAt) {}
