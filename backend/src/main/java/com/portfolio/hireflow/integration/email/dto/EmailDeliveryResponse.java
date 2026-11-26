package com.portfolio.hireflow.integration.email.dto;

import com.portfolio.hireflow.integration.email.entity.*;
import java.time.Instant;

public record EmailDeliveryResponse(
    Long id,
    EmailMessageType messageType,
    String referenceType,
    Long referenceId,
    String recipientEmail,
    String subject,
    EmailDeliveryStatus status,
    int retryCount,
    Instant nextRetryAt,
    Instant sentAt,
    String lastError,
    Instant createdAt) {}
