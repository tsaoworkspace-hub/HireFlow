package com.portfolio.hireflow.interview.controller;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import com.portfolio.hireflow.common.response.ApiResponse;
import com.portfolio.hireflow.interview.dto.InterviewTimeSlotResponse;
import com.portfolio.hireflow.interview.service.InterviewSchedulingService;
@RestController @RequestMapping("/api/public/applications/{publicId}/interview-slots")
public class PublicInterviewTimeSlotController{
 private final InterviewSchedulingService service; public PublicInterviewTimeSlotController(InterviewSchedulingService service){this.service=service;}
 @GetMapping public ApiResponse<List<InterviewTimeSlotResponse>> find(@PathVariable UUID publicId,@RequestParam String token){return ApiResponse.ok("已取得可選面試時段",service.findPublic(publicId,token));}
 @PostMapping("/{slotId}/select") public ApiResponse<InterviewTimeSlotResponse> select(@PathVariable UUID publicId,@PathVariable Long slotId,@RequestParam String token){return ApiResponse.ok("面試時間已確認",service.selectPublic(publicId,slotId,token));}
}
