package com.portfolio.hireflow.auth.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.auth.dto.CurrentUserResponse;
import com.portfolio.hireflow.auth.dto.LoginRequest;
import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.common.exception.UnauthorizedException;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            AppUserRepository appUserRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.appUserRepository = appUserRepository;
    }

    public Authentication authenticate(LoginRequest loginRequest) {
        try {
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            loginRequest.email().trim().toLowerCase(),
                            loginRequest.password()
                    )
            );
        } catch (AuthenticationException exception) {
            throw new UnauthorizedException("帳號或密碼錯誤");
        }
    }

    public CurrentUserResponse getCurrentUser(String email) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("登入狀態無效"));
        return toResponse(user);
    }

    private CurrentUserResponse toResponse(AppUser user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getCode())
                .collect(Collectors.toUnmodifiableSet());
        String department = user.getDepartment() == null ? null : user.getDepartment().getName();
        return new CurrentUserResponse(user.getId(), user.getEmail(), user.getDisplayName(), department, roles);
    }
}
