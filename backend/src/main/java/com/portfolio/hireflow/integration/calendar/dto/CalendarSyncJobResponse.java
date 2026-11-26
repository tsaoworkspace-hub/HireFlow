package com.portfolio.hireflow.integration.calendar.dto;

import com.portfolio.hireflow.integration.calendar.entity.*;
import java.time.Instant;

public record CalendarSyncJobResponse(
    Long id,
    Long interviewId,
    Long userId,
    String userName,
    CalendarSyncOperation operation,
    CalendarSyncStatus status,
    int retryCount,
    Instant nextRetryAt,
    String lastError,
    Instant completedAt,
    Instant createdAt) {}
