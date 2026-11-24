package com.portfolio.hireflow.interview.entity;

import java.time.Instant;

import com.portfolio.hireflow.auth.entity.AppUser;

import jakarta.persistence.*;

@Entity
@Table(
        name = "interview_scorecards",
        uniqueConstraints = @UniqueConstraint(columnNames = {"interview_id", "interviewer_id"})
)
public class InterviewScorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id")
    private Interview interview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interviewer_id")
    private AppUser interviewer;

    @Column(name = "technical_score", nullable = false)
    private int technicalScore;

    @Column(name = "problem_analysis_score", nullable = false)
    private int problemAnalysisScore;

    @Column(name = "communication_score", nullable = false)
    private int communicationScore;

    @Column(name = "learning_ability_score", nullable = false)
    private int learningAbilityScore;

    @Column(name = "overall_comment", nullable = false, length = 3000)
    private String overallComment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ScorecardRecommendation recommendation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScorecardStatus status = ScorecardStatus.DRAFT;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "unlocked_at")
    private Instant unlockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unlocked_by_id")
    private AppUser unlockedBy;

    @Column(name = "unlock_reason", length = 1000)
    private String unlockReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected InterviewScorecard() {
    }

    public InterviewScorecard(Interview interview, AppUser interviewer) {
        this.interview = interview;
        this.interviewer = interviewer;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(
            int technicalScore,
            int problemAnalysisScore,
            int communicationScore,
            int learningAbilityScore,
            String overallComment,
            ScorecardRecommendation recommendation
    ) {
        this.technicalScore = technicalScore;
        this.problemAnalysisScore = problemAnalysisScore;
        this.communicationScore = communicationScore;
        this.learningAbilityScore = learningAbilityScore;
        this.overallComment = overallComment.trim();
        this.recommendation = recommendation;
        this.updatedAt = Instant.now();
    }

    public void submit() {
        this.status = ScorecardStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.updatedAt = this.submittedAt;
    }

    public void unlock(AppUser actor, String reason) {
        this.status = ScorecardStatus.DRAFT;
        this.unlockedAt = Instant.now();
        this.unlockedBy = actor;
        this.unlockReason = reason.trim();
        this.updatedAt = this.unlockedAt;
    }

    public Long getId() { return id; }
    public Interview getInterview() { return interview; }
    public AppUser getInterviewer() { return interviewer; }
    public int getTechnicalScore() { return technicalScore; }
    public int getProblemAnalysisScore() { return problemAnalysisScore; }
    public int getCommunicationScore() { return communicationScore; }
    public int getLearningAbilityScore() { return learningAbilityScore; }
    public String getOverallComment() { return overallComment; }
    public ScorecardRecommendation getRecommendation() { return recommendation; }
    public ScorecardStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getUnlockedAt() { return unlockedAt; }
    public AppUser getUnlockedBy() { return unlockedBy; }
    public String getUnlockReason() { return unlockReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
