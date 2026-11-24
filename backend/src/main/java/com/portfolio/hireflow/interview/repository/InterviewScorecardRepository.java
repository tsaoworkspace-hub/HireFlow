package com.portfolio.hireflow.interview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portfolio.hireflow.interview.entity.InterviewScorecard;
import com.portfolio.hireflow.interview.entity.ScorecardStatus;

public interface InterviewScorecardRepository extends JpaRepository<InterviewScorecard, Long> {

    @EntityGraph(attributePaths = {"interviewer", "unlockedBy"})
    List<InterviewScorecard> findByInterviewIdOrderByCreatedAtAsc(Long interviewId);

    Optional<InterviewScorecard> findByInterviewIdAndInterviewerId(Long interviewId, Long interviewerId);

    long countByInterviewIdAndStatus(Long interviewId, ScorecardStatus status);

    @Query("""
            select count(scorecard) from InterviewScorecard scorecard
            where scorecard.interview.application.id = :applicationId
              and scorecard.status = :status
            """)
    long countByApplicationIdAndStatus(
            @Param("applicationId") Long applicationId,
            @Param("status") ScorecardStatus status
    );

    @EntityGraph(attributePaths = {"interview", "interviewer"})
    List<InterviewScorecard> findByInterviewApplicationIdAndStatusOrderByInterviewRoundNumberAscCreatedAtAsc(
            Long applicationId,
            ScorecardStatus status
    );
}
