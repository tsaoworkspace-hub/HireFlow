package com.portfolio.hireflow.integration.calendar.dto;import java.time.Instant;import com.portfolio.hireflow.integration.calendar.entity.*;
public record CalendarSyncJobResponse(Long id,Long interviewId,Long userId,String userName,CalendarSyncOperation operation,CalendarSyncStatus status,int retryCount,Instant nextRetryAt,String lastError,Instant completedAt,Instant createdAt){}
