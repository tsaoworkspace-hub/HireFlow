package com.portfolio.hireflow.requisition.entity;

import java.time.Instant;

import com.portfolio.hireflow.auth.entity.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "requisition_actions")
public class RequisitionAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisition_id", nullable = false)
    private JobRequisition requisition;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private RequisitionActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private RequisitionStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private RequisitionStatus toStatus;

    @Column(length = 1000)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private AppUser actor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RequisitionAction() {
    }

    public RequisitionAction(
            JobRequisition requisition,
            RequisitionActionType actionType,
            RequisitionStatus fromStatus,
            RequisitionStatus toStatus,
            String reason,
            AppUser actor
    ) {
        this.requisition = requisition;
        this.actionType = actionType;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
        this.actor = actor;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public RequisitionActionType getActionType() { return actionType; }
    public RequisitionStatus getFromStatus() { return fromStatus; }
    public RequisitionStatus getToStatus() { return toStatus; }
    public String getReason() { return reason; }
    public AppUser getActor() { return actor; }
    public Instant getCreatedAt() { return createdAt; }
}
