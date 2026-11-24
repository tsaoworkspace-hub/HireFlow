package com.portfolio.hireflow.candidate.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record CandidateResumeResponse(Long id, String fullName, String email, String phone,
        String currentPosition, BigDecimal yearsExperience, String skills, String address,
        String autobiography, String portfolioUrl, List<WorkExperienceResponse> workExperiences) {
    public record WorkExperienceResponse(String companyName, String jobTitle, LocalDate startDate,
            LocalDate endDate, boolean currentJob, String description) {}
}
