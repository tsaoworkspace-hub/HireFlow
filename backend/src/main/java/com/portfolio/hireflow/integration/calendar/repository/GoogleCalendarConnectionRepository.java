package com.portfolio.hireflow.integration.calendar.repository;

import com.portfolio.hireflow.integration.calendar.entity.GoogleCalendarConnection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleCalendarConnectionRepository
    extends JpaRepository<GoogleCalendarConnection, Long> {
  Optional<GoogleCalendarConnection> findByUserId(Long userId);

  boolean existsByUserId(Long userId);

  void deleteByUserId(Long userId);
}
