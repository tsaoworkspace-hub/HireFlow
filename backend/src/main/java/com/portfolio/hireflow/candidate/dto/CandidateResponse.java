package com.portfolio.hireflow.candidate.dto;
import java.math.BigDecimal; import java.time.Instant;
public record CandidateResponse(Long id,String fullName,String email,String phone,String currentPosition,
 BigDecimal yearsExperience,String skills,String notes,long version,Instant createdAt,Instant updatedAt) {}
