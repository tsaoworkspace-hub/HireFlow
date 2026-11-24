package com.portfolio.hireflow.offer.entity;

import java.time.Instant;

import com.portfolio.hireflow.auth.entity.AppUser;
import jakarta.persistence.*;

@Entity
@Table(name = "job_offer_histories")
public class JobOfferHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id")
    private JobOffer offer;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private JobOfferStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private JobOfferStatus toStatus;

    @Column(length = 2000)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private OfferActorType actorType;

    @Column(name = "actor_label", nullable = false, length = 120)
    private String actorLabel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected JobOfferHistory() {}

    public JobOfferHistory(JobOffer offer, JobOfferStatus from, JobOfferStatus to, String reason,
            AppUser actor, OfferActorType actorType, String actorLabel) {
        this.offer = offer;
        this.fromStatus = from;
        this.toStatus = to;
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
        this.actor = actor;
        this.actorType = actorType;
        this.actorLabel = actorLabel;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public JobOfferStatus getFromStatus() { return fromStatus; }
    public JobOfferStatus getToStatus() { return toStatus; }
    public String getReason() { return reason; }
    public String getActorLabel() { return actorLabel; }
    public OfferActorType getActorType() { return actorType; }
    public Instant getCreatedAt() { return createdAt; }
}
