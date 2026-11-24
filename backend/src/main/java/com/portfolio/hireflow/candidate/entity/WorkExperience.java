package com.portfolio.hireflow.candidate.entity;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "work_experiences")
public class WorkExperience {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "candidate_id") private Candidate candidate;
    @Column(name = "company_name", nullable = false, length = 150) private String companyName;
    @Column(name = "job_title", nullable = false, length = 150) private String jobTitle;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "current_job", nullable = false) private boolean currentJob;
    @Column(length = 2000) private String description;
    @Column(name = "sort_order", nullable = false) private int sortOrder;

    protected WorkExperience() {}
    public WorkExperience(Candidate candidate, String companyName, String jobTitle, LocalDate startDate,
                          LocalDate endDate, boolean currentJob, String description, int sortOrder) {
        this.candidate = candidate; this.companyName = companyName.trim(); this.jobTitle = jobTitle.trim();
        this.startDate = startDate; this.endDate = currentJob ? null : endDate; this.currentJob = currentJob;
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.sortOrder = sortOrder;
    }
    public String getCompanyName() { return companyName; }
    public String getJobTitle() { return jobTitle; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isCurrentJob() { return currentJob; }
    public String getDescription() { return description; }
}
