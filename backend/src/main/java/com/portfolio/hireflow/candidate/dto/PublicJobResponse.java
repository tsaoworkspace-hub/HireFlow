package com.portfolio.hireflow.candidate.dto;
import java.math.BigDecimal;
import java.time.Instant;
public record PublicJobResponse(String slug, String title, String department, String location,
        String employmentType, String description, String requiredSkills, BigDecimal salaryMin,
        BigDecimal salaryMax, Instant publishedAt) {}
