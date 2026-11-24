package com.portfolio.hireflow.interview.dto;

import com.portfolio.hireflow.interview.entity.ScorecardRecommendation;

import jakarta.validation.constraints.*;

public record ScorecardRequest(
        @NotNull @Min(1) @Max(5) Integer technicalScore,
        @NotNull @Min(1) @Max(5) Integer problemAnalysisScore,
        @NotNull @Min(1) @Max(5) Integer communicationScore,
        @NotNull @Min(1) @Max(5) Integer learningAbilityScore,
        @NotBlank @Size(max = 3000) String overallComment,
        @NotNull ScorecardRecommendation recommendation,
        boolean submit
) {
}
