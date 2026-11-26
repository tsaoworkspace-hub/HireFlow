package com.portfolio.hireflow.interview.dto;

import java.time.Instant;
import com.portfolio.hireflow.interview.entity.InterviewFormat;
import jakarta.validation.constraints.*;

public record RescheduleInterviewRequest(
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotNull InterviewFormat format,
        @Size(max = 255) String location,
        @NotBlank @Size(max = 1000) String reason,
        @NotNull @PositiveOrZero Long version
) {}
