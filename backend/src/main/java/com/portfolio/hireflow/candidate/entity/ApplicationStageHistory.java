package com.portfolio.hireflow.candidate.entity;

import java.time.Instant;
import com.portfolio.hireflow.auth.entity.AppUser;
import jakarta.persistence.*;

@Entity
@Table(name = "application_stage_histories")
public class ApplicationStageHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "application_id")
    private Application application;
    @Enumerated(EnumType.STRING) @Column(name = "from_stage", length = 40)
    private ApplicationStage fromStage;
    @Enumerated(EnumType.STRING) @Column(name = "to_stage", nullable = false, length = 40)
    private ApplicationStage toStage;
    @Column(length = 1000) private String reason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "actor_id")
    private AppUser actor;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    protected ApplicationStageHistory() {}
    public ApplicationStageHistory(Application application, ApplicationStage from, ApplicationStage to, String reason, AppUser actor) {
        this.application=application; this.fromStage=from; this.toStage=to;
        this.reason=reason == null || reason.isBlank() ? null : reason.trim(); this.actor=actor; this.createdAt=Instant.now();
    }
    public Long getId(){return id;} public ApplicationStage getFromStage(){return fromStage;}
    public ApplicationStage getToStage(){return toStage;} public String getReason(){return reason;}
    public AppUser getActor(){return actor;} public Instant getCreatedAt(){return createdAt;}
}
