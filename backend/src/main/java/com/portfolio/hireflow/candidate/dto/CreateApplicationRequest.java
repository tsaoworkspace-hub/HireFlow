package com.portfolio.hireflow.candidate.dto;
import jakarta.validation.constraints.NotNull;
public record CreateApplicationRequest(@NotNull Long candidateId,@NotNull Long jobOpeningId) {}
