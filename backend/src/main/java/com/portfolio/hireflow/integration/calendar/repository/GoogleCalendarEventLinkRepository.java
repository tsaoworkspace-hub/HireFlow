package com.portfolio.hireflow.integration.calendar.repository;

import com.portfolio.hireflow.integration.calendar.entity.GoogleCalendarEventLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleCalendarEventLinkRepository
    extends JpaRepository<GoogleCalendarEventLink, Long> {
  Optional<GoogleCalendarEventLink> findByInterviewIdAndUserId(Long interviewId, Long userId);
}
