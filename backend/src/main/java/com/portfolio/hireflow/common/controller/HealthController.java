package com.portfolio.hireflow.common.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.hireflow.common.response.ApiResponse;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok("HireFlow API is ready", Map.of("status", "UP"));
    }
}

