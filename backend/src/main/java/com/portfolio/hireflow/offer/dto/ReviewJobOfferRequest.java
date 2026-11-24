package com.portfolio.hireflow.offer.dto;

import com.portfolio.hireflow.offer.entity.OfferReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewJobOfferRequest(
        @NotNull OfferReviewDecision decision,
        @Size(max = 2000) String reason
) {}
