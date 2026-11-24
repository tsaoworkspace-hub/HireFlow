package com.portfolio.hireflow.requisition.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "job_requisitions")
public class JobRequisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    private Integer headcount;

    @Column(length = 1000)
    private String reason;

    @Column(name = "required_skills", length = 2000)
    private String requiredSkills;

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "target_start_date")
    private LocalDate targetStartDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequisitionStatus status = RequisitionStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private AppUser createdBy;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected JobRequisition() {
    }

    public JobRequisition(Department department, AppUser createdBy) {
        this.department = department;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(
            Department department,
            String jobTitle,
            Integer headcount,
            String reason,
            String requiredSkills,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            LocalDate targetStartDate
    ) {
        this.department = department;
        this.jobTitle = normalize(jobTitle);
        this.headcount = headcount;
        this.reason = normalize(reason);
        this.requiredSkills = normalize(requiredSkills);
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.targetStartDate = targetStartDate;
        this.updatedAt = Instant.now();
    }

    public void submit() {
        this.status = RequisitionStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.reviewedAt = null;
        this.updatedAt = this.submittedAt;
    }

    public void approve() {
        reviewAs(RequisitionStatus.APPROVED);
    }

    public void returnForRevision() {
        reviewAs(RequisitionStatus.RETURNED);
    }

    public void reject() {
        reviewAs(RequisitionStatus.REJECTED);
    }

    private void reviewAs(RequisitionStatus nextStatus) {
        this.status = nextStatus;
        this.reviewedAt = Instant.now();
        this.updatedAt = this.reviewedAt;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public Long getId() { return id; }
    public Department getDepartment() { return department; }
    public String getJobTitle() { return jobTitle; }
    public Integer getHeadcount() { return headcount; }
    public String getReason() { return reason; }
    public String getRequiredSkills() { return requiredSkills; }
    public BigDecimal getSalaryMin() { return salaryMin; }
    public BigDecimal getSalaryMax() { return salaryMax; }
    public LocalDate getTargetStartDate() { return targetStartDate; }
    public RequisitionStatus getStatus() { return status; }
    public AppUser getCreatedBy() { return createdBy; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}
