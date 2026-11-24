package com.portfolio.hireflow.candidate.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record PublicApplicationRequest(
        @NotBlank @Size(max=100) String fullName,
        @NotBlank @Email @Size(max=255) String email,
        @Size(max=40) String phone,
        @Size(max=120) String currentPosition,
        @DecimalMin("0.0") BigDecimal yearsExperience,
        @Size(max=2000) String skills,
        @Size(max=300) String address,
        @Size(max=5000) String autobiography,
        @Size(max=500) String portfolioUrl,
        @AssertTrue(message="必須同意個人資料蒐集與招募使用") boolean consent,
        @Valid List<WorkExperienceRequest> workExperiences) {
    public record WorkExperienceRequest(@NotBlank @Size(max=150) String companyName,
            @NotBlank @Size(max=150) String jobTitle, LocalDate startDate, LocalDate endDate,
            boolean currentJob, @Size(max=2000) String description) {}
}
