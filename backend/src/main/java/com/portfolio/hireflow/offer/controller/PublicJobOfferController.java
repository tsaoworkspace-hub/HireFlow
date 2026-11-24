package com.portfolio.hireflow.offer.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.*;

import com.portfolio.hireflow.common.response.ApiResponse;
import com.portfolio.hireflow.offer.dto.OfferActionRequest;
import com.portfolio.hireflow.offer.dto.PublicJobOfferResponse;
import com.portfolio.hireflow.offer.service.JobOfferService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/public/applications/{publicId}/offer")
public class PublicJobOfferController {
    private final JobOfferService service;

    public PublicJobOfferController(JobOfferService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PublicJobOfferResponse> find(@PathVariable UUID publicId, @RequestParam String token) {
        return ApiResponse.ok("已取得 Offer", service.publicOffer(publicId, token));
    }

    @PostMapping("/accept")
    public ApiResponse<PublicJobOfferResponse> accept(@PathVariable UUID publicId, @RequestParam String token) {
        return ApiResponse.ok("你已接受 Offer", service.respond(publicId, token, true, null));
    }

    @PostMapping("/decline")
    public ApiResponse<PublicJobOfferResponse> decline(@PathVariable UUID publicId, @RequestParam String token,
            @Valid @RequestBody OfferActionRequest request) {
        return ApiResponse.ok("你已拒絕 Offer", service.respond(publicId, token, false, request.reason()));
    }
}
