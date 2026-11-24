package com.portfolio.hireflow.candidate.dto;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
public record CandidateRequest(
 @NotBlank(message="姓名為必填") @Size(max=100) String fullName,
 @NotBlank(message="Email 為必填") @Email(message="Email 格式不正確") @Size(max=255) String email,
 @Size(max=40) String phone, @Size(max=120) String currentPosition,
 @DecimalMin(value="0", message="年資不可小於 0") BigDecimal yearsExperience,
 @Size(max=2000) String skills, @Size(max=2000) String notes, Long version) {}
