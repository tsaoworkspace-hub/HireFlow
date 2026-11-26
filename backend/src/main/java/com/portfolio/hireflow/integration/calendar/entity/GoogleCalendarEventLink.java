package com.portfolio.hireflow.integration.calendar.entity;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.interview.entity.Interview;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "google_calendar_event_links")
public class GoogleCalendarEventLink {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "interview_id")
  private Interview interview;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private AppUser user;

  @Column(name = "google_event_id", nullable = false, length = 500)
  private String googleEventId;

  @Column(name = "synced_at", nullable = false)
  private Instant syncedAt;

  protected GoogleCalendarEventLink() {}

  public GoogleCalendarEventLink(Interview i, AppUser u, String eventId) {
    interview = i;
    user = u;
    googleEventId = eventId;
    syncedAt = Instant.now();
  }

  public void synced() {
    syncedAt = Instant.now();
  }

  public String getGoogleEventId() {
    return googleEventId;
  }
}
