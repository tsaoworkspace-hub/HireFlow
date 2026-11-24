package com.portfolio.hireflow.audit.entity;

import java.time.Instant;

import com.portfolio.hireflow.auth.entity.AppUser;
import jakarta.persistence.*;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private AuditTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditOutcome outcome;

    @Column(length = 2000)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(
            AppUser actor,
            AuditAction action,
            AuditTargetType targetType,
            Long targetId,
            String details
    ) {
        this.actor = actor;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.outcome = AuditOutcome.SUCCESS;
        this.details = normalize(details);
        this.createdAt = Instant.now();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public AppUser getActor() { return actor; }
    public AuditAction getAction() { return action; }
    public AuditTargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public AuditOutcome getOutcome() { return outcome; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
