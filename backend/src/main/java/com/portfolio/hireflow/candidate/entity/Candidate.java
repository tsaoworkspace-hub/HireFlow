package com.portfolio.hireflow.candidate.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "candidates")
public class Candidate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    @Column(nullable = false, length = 255)
    private String email;
    @Column(length = 40)
    private String phone;
    @Column(name = "current_position", length = 120)
    private String currentPosition;
    @Column(name = "years_experience", precision = 4, scale = 1)
    private BigDecimal yearsExperience;
    @Column(length = 2000)
    private String skills;
    @Column(length = 2000)
    private String notes;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version private long version;

    protected Candidate() {}
    public Candidate(String fullName, String email) {
        this.fullName = fullName.trim();
        this.email = email.trim().toLowerCase();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
    public void update(String fullName, String email, String phone, String currentPosition,
                       BigDecimal yearsExperience, String skills, String notes) {
        this.fullName = fullName.trim();
        this.email = email.trim().toLowerCase();
        this.phone = normalize(phone);
        this.currentPosition = normalize(currentPosition);
        this.yearsExperience = yearsExperience;
        this.skills = normalize(skills);
        this.notes = normalize(notes);
        this.updatedAt = Instant.now();
    }
    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getCurrentPosition() { return currentPosition; }
    public BigDecimal getYearsExperience() { return yearsExperience; }
    public String getSkills() { return skills; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
