package com.portfolio.hireflow.integration.email.controller;
import org.springframework.data.domain.*;import org.springframework.data.web.PageableDefault;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
import com.portfolio.hireflow.common.response.*;import com.portfolio.hireflow.integration.email.dto.EmailDeliveryResponse;import com.portfolio.hireflow.integration.email.service.EmailDeliveryAdminService;
@RestController @RequestMapping("/api/integrations/email-deliveries") @PreAuthorize("hasAnyRole('ADMIN','HR')")
public class EmailDeliveryController{private final EmailDeliveryAdminService service;public EmailDeliveryController(EmailDeliveryAdminService service){this.service=service;}
 @GetMapping public ApiResponse<PageResponse<EmailDeliveryResponse>> find(Authentication a,@PageableDefault(size=50,sort="createdAt",direction=Sort.Direction.DESC) Pageable p){return ApiResponse.ok("已取得 Email 寄送紀錄",service.findAll(a.getName(),p));}
 @PostMapping("/{id}/retry") public ApiResponse<EmailDeliveryResponse> retry(@PathVariable Long id,Authentication a){return ApiResponse.ok("Email 已排入重送",service.retry(id,a.getName()));}}
