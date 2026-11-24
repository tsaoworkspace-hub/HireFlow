package com.portfolio.hireflow.interview.controller;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.portfolio.hireflow.common.response.ApiResponse;
import com.portfolio.hireflow.interview.dto.*;
import com.portfolio.hireflow.interview.service.InterviewSchedulingService;
import jakarta.validation.Valid;
@RestController @RequestMapping("/api/interview-slots") @PreAuthorize("hasAnyRole('ADMIN','HR','HIRING_MANAGER')")
public class InterviewTimeSlotController{
 private final InterviewSchedulingService service; public InterviewTimeSlotController(InterviewSchedulingService service){this.service=service;}
 @GetMapping public ApiResponse<List<InterviewTimeSlotResponse>> find(@RequestParam Long applicationId,Authentication a){return ApiResponse.ok("已取得面試時段",service.findForApplication(applicationId,a.getName()));}
 @PostMapping @PreAuthorize("hasAnyRole('ADMIN','HR')") public ResponseEntity<ApiResponse<InterviewTimeSlotResponse>> create(@Valid @RequestBody CreateInterviewTimeSlotRequest r,Authentication a){return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("面試時段已提供",service.propose(a.getName(),r)));}
 @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','HR')") public ApiResponse<Void> withdraw(@PathVariable Long id,Authentication a){service.withdraw(id,a.getName());return ApiResponse.ok("面試時段已撤回",null);}
}
