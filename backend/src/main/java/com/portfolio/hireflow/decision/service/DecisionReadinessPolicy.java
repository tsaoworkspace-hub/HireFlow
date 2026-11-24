package com.portfolio.hireflow.decision.service;

import org.springframework.stereotype.Component;

import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.interview.entity.InterviewStatus;
import com.portfolio.hireflow.interview.entity.ScorecardStatus;
import com.portfolio.hireflow.interview.repository.InterviewRepository;
import com.portfolio.hireflow.interview.repository.InterviewScorecardRepository;

@Component
public class DecisionReadinessPolicy {

    private final InterviewRepository interviews;
    private final InterviewScorecardRepository scorecards;

    public DecisionReadinessPolicy(
            InterviewRepository interviews,
            InterviewScorecardRepository scorecards
    ) {
        this.interviews = interviews;
        this.scorecards = scorecards;
    }

    public boolean isReady(Long applicationId) {
        return interviews.countByApplicationIdAndStatus(applicationId, InterviewStatus.COMPLETED) > 0
                && interviews.countByApplicationIdAndStatus(applicationId, InterviewStatus.SCHEDULED) == 0
                && scorecards.countByApplicationIdAndStatus(applicationId, ScorecardStatus.SUBMITTED) > 0;
    }

    public void verify(Long applicationId) {
        if (interviews.countByApplicationIdAndStatus(applicationId, InterviewStatus.COMPLETED) < 1) {
            throw new BusinessException("至少需要一場已完成的面試才能進入決策");
        }
        if (interviews.countByApplicationIdAndStatus(applicationId, InterviewStatus.SCHEDULED) > 0) {
            throw new BusinessException("仍有尚未完成的面試，不能進入決策");
        }
        if (scorecards.countByApplicationIdAndStatus(applicationId, ScorecardStatus.SUBMITTED) < 1) {
            throw new BusinessException("至少需要一份已送出的評分才能進入決策");
        }
    }
}
