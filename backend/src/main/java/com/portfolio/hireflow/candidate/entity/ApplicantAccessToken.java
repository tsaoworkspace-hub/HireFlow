package com.portfolio.hireflow.candidate.entity;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "applicant_access_tokens")
public class ApplicantAccessToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "application_id")
    private Application application;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "last_accessed_at") private Instant lastAccessedAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected ApplicantAccessToken() {}
    public ApplicantAccessToken(Application application, String tokenHash, Instant expiresAt) {
        this.application = application; this.tokenHash = tokenHash; this.expiresAt = expiresAt; this.createdAt = Instant.now();
    }
    public boolean isUsableAt(Instant now) { return revokedAt == null && expiresAt.isAfter(now); }
    public void markAccessed() { this.lastAccessedAt = Instant.now(); }
    public Application getApplication() { return application; }
}
