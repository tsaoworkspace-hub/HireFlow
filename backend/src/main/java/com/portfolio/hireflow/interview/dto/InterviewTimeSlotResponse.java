package com.portfolio.hireflow.interview.dto;
import java.time.Instant;
import java.util.List;
import com.portfolio.hireflow.interview.entity.*;
public record InterviewTimeSlotResponse(Long id,Long applicationId,String candidateName,String jobTitle,String departmentName,
 int roundNumber,Instant startsAt,Instant endsAt,InterviewFormat format,String location,InterviewTimeSlotStatus status,
 List<InterviewerResponse> interviewers,Long interviewId,long version){}
