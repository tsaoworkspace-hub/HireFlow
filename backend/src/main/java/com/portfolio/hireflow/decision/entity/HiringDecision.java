package com.portfolio.hireflow.decision.entity;

import java.time.Instant;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.candidate.entity.Application;
import jakarta.persistence.*;

@Entity
@Table(name = "hiring_decisions", uniqueConstraints = @UniqueConstraint(columnNames = "application_id"))
public class HiringDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DecisionResult result;

    @Column(nullable = false, length = 2000)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decided_by_id")
    private AppUser decidedBy;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    protected HiringDecision() {
    }

    public HiringDecision(Application application, DecisionResult result, String reason, AppUser decidedBy) {
        this.application = application;
        this.result = result;
        this.reason = reason.trim();
        this.decidedBy = decidedBy;
        this.decidedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Application getApplication() { return application; }
    public DecisionResult getResult() { return result; }
    public String getReason() { return reason; }
    public AppUser getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
}
