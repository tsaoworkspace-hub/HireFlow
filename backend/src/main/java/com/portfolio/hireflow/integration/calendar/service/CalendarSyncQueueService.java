package com.portfolio.hireflow.integration.calendar.service;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.integration.calendar.entity.*;
import com.portfolio.hireflow.integration.calendar.repository.*;
import com.portfolio.hireflow.interview.entity.Interview;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarSyncQueueService {
  private final CalendarSyncJobRepository jobs;
  private final GoogleCalendarConnectionRepository connections;

  public CalendarSyncQueueService(
      CalendarSyncJobRepository jobs, GoogleCalendarConnectionRepository connections) {
    this.jobs = jobs;
    this.connections = connections;
  }

  @Transactional
  public void queueUpserts(Interview interview) {
    participants(interview).forEach(u -> queue(interview, u, CalendarSyncOperation.UPSERT));
  }

  @Transactional
  public void queueDeletes(Interview interview) {
    participants(interview).forEach(u -> queue(interview, u, CalendarSyncOperation.DELETE));
  }

  private void queue(Interview i, AppUser u, CalendarSyncOperation op) {
    if (!connections.existsByUserId(u.getId())) return;
    String key = "interview-" + i.getId() + "-user-" + u.getId() + "-" + op + "-v" + i.getVersion();
    if (!jobs.existsByKey(key)) jobs.save(new CalendarSyncJob(key, i, u, op));
  }

  private Set<AppUser> participants(Interview i) {
    Set<AppUser> p = new HashSet<>(i.getInterviewers());
    p.add(i.getCreatedBy());
    return p;
  }
}
