package com.portfolio.hireflow.offer.dto;

import jakarta.validation.constraints.Size;

public record OfferActionRequest(@Size(max = 2000) String reason) {}
