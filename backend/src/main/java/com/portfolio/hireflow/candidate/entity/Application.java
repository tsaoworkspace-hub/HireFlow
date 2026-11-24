package com.portfolio.hireflow.candidate.entity;

import java.time.Instant;
import java.util.UUID;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.requisition.entity.JobOpening;
import jakarta.persistence.*;

@Entity
@Table(name = "applications", uniqueConstraints = @UniqueConstraint(columnNames = {"candidate_id", "job_opening_id"}))
public class Application {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "candidate_id")
    private Candidate candidate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "job_opening_id")
    private JobOpening jobOpening;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ApplicationStage stage = ApplicationStage.NEW;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_id")
    private AppUser createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version private long version;

    protected Application() {}
    public Application(Candidate candidate, JobOpening jobOpening, AppUser createdBy) {
        this.candidate = candidate; this.jobOpening = jobOpening; this.createdBy = createdBy;
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    public void transitionTo(ApplicationStage next) { this.stage = next; this.updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Candidate getCandidate() { return candidate; }
    public JobOpening getJobOpening() { return jobOpening; }
    public ApplicationStage getStage() { return stage; }
    public AppUser getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
