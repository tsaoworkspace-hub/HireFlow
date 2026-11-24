package com.portfolio.hireflow.offer.dto;

import java.time.Instant;

import com.portfolio.hireflow.offer.entity.JobOfferStatus;
import com.portfolio.hireflow.offer.entity.OfferActorType;

public record JobOfferHistoryResponse(
        Long id,
        JobOfferStatus fromStatus,
        JobOfferStatus toStatus,
        String reason,
        OfferActorType actorType,
        String actorLabel,
        Instant createdAt
) {}
