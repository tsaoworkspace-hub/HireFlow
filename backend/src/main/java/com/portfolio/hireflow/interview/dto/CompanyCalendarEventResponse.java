package com.portfolio.hireflow.interview.dto;import java.time.Instant;import com.portfolio.hireflow.interview.entity.*;
public record CompanyCalendarEventResponse(String id,String eventType,Long applicationId,String candidateName,String jobTitle,String departmentName,Instant startsAt,Instant endsAt,InterviewFormat format,String location,String status){}
