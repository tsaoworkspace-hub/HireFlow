package com.portfolio.hireflow.requisition.entity;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.entity.Department;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "job_openings")
public class JobOpening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisition_id", nullable = false, unique = true)
    private JobRequisition requisition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "job_title", nullable = false, length = 120)
    private String jobTitle;

    @Column(nullable = false)
    private int headcount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobOpeningStatus status = JobOpeningStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsible_hr_id", nullable = false)
    private AppUser responsibleHr;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    private long version;

    @Column(name = "public_slug", nullable = false, unique = true, length = 180)
    private String publicSlug;

    @Column(name = "public_description", length = 5000)
    private String publicDescription;

    @Column(length = 120)
    private String location;

    @Column(name = "employment_type", length = 60)
    private String employmentType;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected JobOpening() {
    }

    public JobOpening(JobRequisition requisition, AppUser responsibleHr) {
        this.requisition = requisition;
        this.department = requisition.getDepartment();
        this.jobTitle = requisition.getJobTitle();
        this.headcount = requisition.getHeadcount();
        this.responsibleHr = responsibleHr;
        this.createdAt = Instant.now();
        this.publicSlug = slugify(this.jobTitle) + "-" + UUID.randomUUID().toString().substring(0, 8);
        this.publicDescription = requisition.getReason();
        this.employmentType = "全職";
        this.publishedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public JobRequisition getRequisition() { return requisition; }
    public Department getDepartment() { return department; }
    public String getJobTitle() { return jobTitle; }
    public int getHeadcount() { return headcount; }
    public AppUser getResponsibleHr() { return responsibleHr; }
    public JobOpeningStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClosedAt() { return closedAt; }
    public long getVersion() { return version; }
    public String getPublicSlug() { return publicSlug; }
    public String getPublicDescription() { return publicDescription; }
    public String getLocation() { return location; }
    public String getEmploymentType() { return employmentType; }
    public Instant getPublishedAt() { return publishedAt; }
    public void close() { this.status = JobOpeningStatus.CLOSED; this.closedAt = Instant.now(); }

    private static String slugify(String value) {
        String slug = value == null ? "job" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\p{IsHan}]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "job" : slug;
    }
}
