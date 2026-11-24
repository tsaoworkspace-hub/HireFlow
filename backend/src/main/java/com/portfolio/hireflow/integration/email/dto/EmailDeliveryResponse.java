package com.portfolio.hireflow.integration.email.dto;
import java.time.Instant;import com.portfolio.hireflow.integration.email.entity.*;
public record EmailDeliveryResponse(Long id,EmailMessageType messageType,String referenceType,Long referenceId,String recipientEmail,String subject,EmailDeliveryStatus status,int retryCount,Instant nextRetryAt,Instant sentAt,String lastError,Instant createdAt){}
