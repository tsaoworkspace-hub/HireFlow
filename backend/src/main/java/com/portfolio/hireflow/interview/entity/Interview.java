package com.portfolio.hireflow.interview.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.candidate.entity.Application;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private Application application;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewFormat format;

    @Column(length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    @ManyToMany
    @BatchSize(size = 50)
    @JoinTable(
            name = "interview_interviewers",
            joinColumns = @JoinColumn(name = "interview_id"),
            inverseJoinColumns = @JoinColumn(name = "interviewer_id")
    )
    private Set<AppUser> interviewers = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Interview() {
    }

    public Interview(
            Application application,
            int roundNumber,
            Instant startsAt,
            Instant endsAt,
            InterviewFormat format,
            String location,
            Set<AppUser> interviewers,
            AppUser createdBy
    ) {
        this.application = application;
        this.roundNumber = roundNumber;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.format = format;
        this.location = normalize(location);
        this.interviewers.addAll(interviewers);
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void cancel(String reason) {
        this.status = InterviewStatus.CANCELLED;
        this.cancellationReason = reason.trim();
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = InterviewStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public Application getApplication() { return application; }
    public int getRoundNumber() { return roundNumber; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public InterviewFormat getFormat() { return format; }
    public String getLocation() { return location; }
    public InterviewStatus getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }
    public Set<AppUser> getInterviewers() { return interviewers; }
    public AppUser getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
