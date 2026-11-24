package com.portfolio.hireflow.interview.dto;
import java.time.Instant;
import java.util.Set;
import com.portfolio.hireflow.interview.entity.InterviewFormat;
import jakarta.validation.constraints.*;
public record CreateInterviewTimeSlotRequest(@NotNull Long applicationId,@NotNull @Positive Integer roundNumber,
 @NotNull Instant startsAt,@NotNull Instant endsAt,@NotNull InterviewFormat format,@Size(max=255) String location,@NotEmpty Set<Long> interviewerIds){}
