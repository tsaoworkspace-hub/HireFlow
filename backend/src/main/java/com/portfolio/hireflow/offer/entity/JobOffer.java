package com.portfolio.hireflow.offer.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.candidate.entity.Application;
import jakarta.persistence.*;

@Entity
@Table(name = "job_offers")
public class JobOffer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", unique = true)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobOfferStatus status = JobOfferStatus.DRAFT;

    @Column(name = "annual_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal annualSalary;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "proposed_start_date", nullable = false)
    private LocalDate proposedStartDate;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id")
    private AppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private AppUser approvedBy;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "response_reason", length = 2000)
    private String responseReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected JobOffer() {}

    public JobOffer(
            Application application,
            BigDecimal annualSalary,
            String currency,
            LocalDate proposedStartDate,
            Instant expiresAt,
            String title,
            String message,
            AppUser createdBy
    ) {
        this.application = application;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
        update(annualSalary, currency, proposedStartDate, expiresAt, title, message);
    }

    public void update(BigDecimal salary, String currency, LocalDate start, Instant expires,
            String title, String message) {
        this.annualSalary = salary;
        this.currency = currency.trim().toUpperCase();
        this.proposedStartDate = start;
        this.expiresAt = expires;
        this.title = title.trim();
        this.message = message.trim();
        this.updatedAt = Instant.now();
    }

    public void submit() {
        status = JobOfferStatus.PENDING_APPROVAL;
        updatedAt = Instant.now();
    }

    public void approve(AppUser actor) {
        status = JobOfferStatus.APPROVED;
        approvedBy = actor;
        updatedAt = Instant.now();
    }

    public void returnForRevision() {
        status = JobOfferStatus.RETURNED;
        approvedBy = null;
        updatedAt = Instant.now();
    }

    public void send() {
        status = JobOfferStatus.SENT;
        sentAt = Instant.now();
        updatedAt = sentAt;
    }

    public void accept() {
        status = JobOfferStatus.ACCEPTED;
        respondedAt = Instant.now();
        updatedAt = respondedAt;
    }

    public void decline(String reason) {
        status = JobOfferStatus.DECLINED;
        responseReason = normalize(reason);
        respondedAt = Instant.now();
        updatedAt = respondedAt;
    }

    public void expire() {
        status = JobOfferStatus.EXPIRED;
        updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        status = JobOfferStatus.CANCELLED;
        responseReason = normalize(reason);
        updatedAt = Instant.now();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public Application getApplication() { return application; }
    public JobOfferStatus getStatus() { return status; }
    public BigDecimal getAnnualSalary() { return annualSalary; }
    public String getCurrency() { return currency; }
    public LocalDate getProposedStartDate() { return proposedStartDate; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public AppUser getCreatedBy() { return createdBy; }
    public AppUser getApprovedBy() { return approvedBy; }
    public Instant getSentAt() { return sentAt; }
    public Instant getRespondedAt() { return respondedAt; }
    public String getResponseReason() { return responseReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
