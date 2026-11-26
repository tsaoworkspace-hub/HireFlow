package com.portfolio.hireflow.integration.calendar.entity;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.interview.entity.Interview;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "calendar_sync_jobs")
public class CalendarSyncJob {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "deduplication_key", nullable = false, unique = true, length = 180)
  private String key;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "interview_id")
  private Interview interview;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private AppUser user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CalendarSyncOperation operation;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CalendarSyncStatus status = CalendarSyncStatus.PENDING;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "next_retry_at")
  private Instant nextRetryAt;

  @Column(name = "last_error", length = 500)
  private String lastError;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CalendarSyncJob() {}

  public CalendarSyncJob(String key, Interview i, AppUser u, CalendarSyncOperation op) {
    this.key = key;
    interview = i;
    user = u;
    operation = op;
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  public void completed() {
    status = CalendarSyncStatus.COMPLETED;
    completedAt = Instant.now();
    lastError = null;
    nextRetryAt = null;
    updatedAt = completedAt;
  }

  public void failed() {
    retryCount++;
    lastError = "Google Calendar 同步失敗，請檢查授權或後端日誌";
    updatedAt = Instant.now();
    if (retryCount >= 3) {
      status = CalendarSyncStatus.FAILED;
      nextRetryAt = null;
    } else {
      status = CalendarSyncStatus.RETRY_WAIT;
      nextRetryAt = Instant.now().plusSeconds(retryCount == 1 ? 60 : 300);
    }
  }

  public void retry() {
    status = CalendarSyncStatus.PENDING;
    nextRetryAt = null;
    lastError = null;
    updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public Interview getInterview() {
    return interview;
  }

  public AppUser getUser() {
    return user;
  }

  public CalendarSyncOperation getOperation() {
    return operation;
  }

  public CalendarSyncStatus getStatus() {
    return status;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public Instant getNextRetryAt() {
    return nextRetryAt;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
