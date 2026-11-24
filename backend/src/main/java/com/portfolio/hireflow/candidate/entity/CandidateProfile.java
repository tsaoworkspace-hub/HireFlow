package com.portfolio.hireflow.candidate.entity;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "candidate_profiles")
public class CandidateProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "candidate_id", unique = true)
    private Candidate candidate;
    @Column(length = 300) private String address;
    @Column(length = 5000) private String autobiography;
    @Column(name = "portfolio_url", length = 500) private String portfolioUrl;
    @Column(name = "consented_at", nullable = false) private Instant consentedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected CandidateProfile() {}
    public CandidateProfile(Candidate candidate) {
        this.candidate = candidate;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
    public void update(String address, String autobiography, String portfolioUrl, Instant consentedAt) {
        this.address = normalize(address);
        this.autobiography = normalize(autobiography);
        this.portfolioUrl = normalize(portfolioUrl);
        this.consentedAt = consentedAt;
        this.updatedAt = Instant.now();
    }
    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public String getAddress() { return address; }
    public String getAutobiography() { return autobiography; }
    public String getPortfolioUrl() { return portfolioUrl; }
}
