package com.portfolio.hireflow.auth.dto;

import java.util.Set;

public record CurrentUserResponse(
        Long id,
        String email,
        String displayName,
        String department,
        Set<String> roles
) {
}
