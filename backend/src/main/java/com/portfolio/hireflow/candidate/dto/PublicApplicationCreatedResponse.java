package com.portfolio.hireflow.candidate.dto;
import java.util.UUID;
public record PublicApplicationCreatedResponse(UUID applicationId, String accessToken, String trackingPath) {}
