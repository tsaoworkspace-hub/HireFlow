package com.portfolio.hireflow.candidate.dto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record CandidatePortalResponse(UUID applicationId, String candidateName, String jobTitle,
        String department, String status, String statusLabel, Instant appliedAt, List<TimelineItem> timeline) {
    public record TimelineItem(String status, String label, Instant occurredAt) {}
}
