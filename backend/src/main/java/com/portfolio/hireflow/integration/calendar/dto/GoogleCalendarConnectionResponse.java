package com.portfolio.hireflow.integration.calendar.dto;

import java.time.Instant;

public record GoogleCalendarConnectionResponse(
    boolean enabled, boolean connected, Instant updatedAt, String authorizationUrl) {}
