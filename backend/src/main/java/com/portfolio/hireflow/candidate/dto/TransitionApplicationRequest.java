package com.portfolio.hireflow.candidate.dto;
import com.portfolio.hireflow.candidate.entity.ApplicationStage;
import jakarta.validation.constraints.*;
public record TransitionApplicationRequest(@NotNull ApplicationStage nextStage,@Size(max=1000) String reason) {}
