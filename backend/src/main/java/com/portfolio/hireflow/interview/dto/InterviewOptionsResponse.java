package com.portfolio.hireflow.interview.dto;

import java.util.List;

public record InterviewOptionsResponse(
        List<InterviewApplicationOptionResponse> applications,
        List<InterviewerResponse> interviewers
) {
}
