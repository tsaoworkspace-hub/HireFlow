package com.portfolio.hireflow.offer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.constraints.*;

public record UpdateJobOfferRequest(
        @NotNull @DecimalMin("1.00") BigDecimal annualSalary,
        @NotBlank @Size(max = 10) String currency,
        @NotNull @FutureOrPresent LocalDate proposedStartDate,
        @NotNull @Future Instant expiresAt,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String message,
        @NotNull Long version
) {}
