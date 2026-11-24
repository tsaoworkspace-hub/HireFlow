package com.portfolio.hireflow.decision.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.audit.entity.AuditAction;
import com.portfolio.hireflow.audit.entity.AuditTargetType;
import com.portfolio.hireflow.audit.service.AuditLogService;
import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.candidate.entity.Application;
import com.portfolio.hireflow.candidate.entity.ApplicationStage;
import com.portfolio.hireflow.candidate.entity.ApplicationStageHistory;
import com.portfolio.hireflow.candidate.repository.ApplicationRepository;
import com.portfolio.hireflow.candidate.repository.ApplicationStageHistoryRepository;
import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.common.exception.ConflictException;
import com.portfolio.hireflow.common.exception.ForbiddenException;
import com.portfolio.hireflow.common.exception.ResourceNotFoundException;
import com.portfolio.hireflow.common.exception.UnauthorizedException;
import com.portfolio.hireflow.decision.dto.*;
import com.portfolio.hireflow.decision.entity.DecisionResult;
import com.portfolio.hireflow.decision.entity.HiringDecision;
import com.portfolio.hireflow.decision.repository.HiringDecisionRepository;
import com.portfolio.hireflow.interview.entity.InterviewScorecard;
import com.portfolio.hireflow.interview.entity.ScorecardStatus;
import com.portfolio.hireflow.interview.repository.InterviewScorecardRepository;

@Service
@Transactional(readOnly = true)
public class HiringDecisionService {

    private final ApplicationRepository applications;
    private final ApplicationStageHistoryRepository histories;
    private final HiringDecisionRepository decisions;
    private final InterviewScorecardRepository scorecards;
    private final AppUserRepository users;
    private final DecisionReadinessPolicy readinessPolicy;
    private final AuditLogService auditLogService;

    public HiringDecisionService(
            ApplicationRepository applications,
            ApplicationStageHistoryRepository histories,
            HiringDecisionRepository decisions,
            InterviewScorecardRepository scorecards,
            AppUserRepository users,
            DecisionReadinessPolicy readinessPolicy,
            AuditLogService auditLogService
    ) {
        this.applications = applications;
        this.histories = histories;
        this.decisions = decisions;
        this.scorecards = scorecards;
        this.users = users;
        this.readinessPolicy = readinessPolicy;
        this.auditLogService = auditLogService;
    }

    public DecisionViewResponse findDecisionView(Long applicationId, String email) {
        AppUser actor = loadActor(email);
        Application application = loadApplication(applicationId);
        ensureCanView(actor, application);
        return toResponse(application, actor);
    }

    @Transactional
    public DecisionViewResponse decide(Long applicationId, String email, CreateDecisionRequest request) {
        AppUser actor = loadActor(email);
        requireDecisionMaker(actor);
        Application application = loadApplication(applicationId);
        ensureCanView(actor, application);

        if (decisions.existsByApplicationId(applicationId)) {
            throw new ConflictException("此應徵紀錄已經完成最終決策");
        }
        if (application.getStage() != ApplicationStage.DECISION_PENDING) {
            throw new BusinessException("只有待決策的應徵紀錄可以建立錄取決策");
        }
        if (request.applicationVersion().longValue() != application.getVersion()) {
            throw new ConflictException("應徵資料已被更新，請重新整理後再決策");
        }
        readinessPolicy.verify(applicationId);

        HiringDecision decision = decisions.save(new HiringDecision(
                application, request.result(), request.reason(), actor));
        ApplicationStage previous = application.getStage();
        ApplicationStage next = mapStage(request.result());
        application.transitionTo(next);
        histories.save(new ApplicationStageHistory(application, previous, next, request.reason(), actor));
        auditLogService.record(
                actor,
                AuditAction.HIRING_DECISION_CREATED,
                AuditTargetType.HIRING_DECISION,
                decision.getId(),
                "Application #" + applicationId + ": " + request.result());

        applications.flush();
        return toResponse(application, actor);
    }

    private DecisionViewResponse toResponse(Application application, AppUser actor) {
        List<DecisionScorecardResponse> scorecardResponses = scorecards
                .findByInterviewApplicationIdAndStatusOrderByInterviewRoundNumberAscCreatedAtAsc(
                        application.getId(), ScorecardStatus.SUBMITTED)
                .stream()
                .map(this::toScorecardResponse)
                .toList();
        HiringDecisionResponse decisionResponse = decisions.findByApplicationId(application.getId())
                .map(this::toDecisionResponse)
                .orElse(null);
        boolean ready = readinessPolicy.isReady(application.getId());
        boolean canDecide = decisionResponse == null
                && ready
                && application.getStage() == ApplicationStage.DECISION_PENDING
                && isDecisionMaker(actor);

        return new DecisionViewResponse(
                application.getId(), application.getCandidate().getId(), application.getCandidate().getFullName(),
                application.getCandidate().getEmail(), application.getJobOpening().getId(),
                application.getJobOpening().getJobTitle(), application.getJobOpening().getDepartment().getId(),
                application.getJobOpening().getDepartment().getName(), application.getStage(),
                application.getVersion(), ready, canDecide, scorecardResponses, decisionResponse);
    }

    private DecisionScorecardResponse toScorecardResponse(InterviewScorecard scorecard) {
        BigDecimal total = BigDecimal.valueOf(
                scorecard.getTechnicalScore()
                        + scorecard.getProblemAnalysisScore()
                        + scorecard.getCommunicationScore()
                        + scorecard.getLearningAbilityScore());
        return new DecisionScorecardResponse(
                scorecard.getId(), scorecard.getInterview().getId(), scorecard.getInterview().getRoundNumber(),
                scorecard.getInterviewer().getId(), scorecard.getInterviewer().getDisplayName(),
                scorecard.getTechnicalScore(), scorecard.getProblemAnalysisScore(),
                scorecard.getCommunicationScore(), scorecard.getLearningAbilityScore(),
                total.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP),
                scorecard.getOverallComment(), scorecard.getRecommendation(), scorecard.getSubmittedAt());
    }

    private HiringDecisionResponse toDecisionResponse(HiringDecision decision) {
        return new HiringDecisionResponse(
                decision.getId(), decision.getResult(), decision.getReason(), decision.getDecidedBy().getId(),
                decision.getDecidedBy().getDisplayName(), decision.getDecidedAt());
    }

    private AppUser loadActor(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("登入狀態無效"));
    }

    private Application loadApplication(Long id) {
        return applications.findOneById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到應徵紀錄"));
    }

    private void ensureCanView(AppUser actor, Application application) {
        if (hasRole(actor, "ADMIN") || hasRole(actor, "HR")) {
            return;
        }
        if (!hasRole(actor, "HIRING_MANAGER") || actor.getDepartment() == null
                || !actor.getDepartment().getId().equals(application.getJobOpening().getDepartment().getId())) {
            throw new ForbiddenException("你只能查看自己部門的決策資料");
        }
    }

    private void requireDecisionMaker(AppUser actor) {
        if (!isDecisionMaker(actor)) {
            throw new ForbiddenException("只有用人主管可以建立錄取決策");
        }
    }

    private boolean isDecisionMaker(AppUser actor) {
        return hasRole(actor, "HIRING_MANAGER") || hasRole(actor, "ADMIN");
    }

    private boolean hasRole(AppUser actor, String code) {
        return actor.getRoles().stream().anyMatch(role -> role.getCode().equals(code));
    }

    private static ApplicationStage mapStage(DecisionResult result) {
        return switch (result) {
            case HIRED -> ApplicationStage.HIRED;
            case ON_HOLD -> ApplicationStage.ON_HOLD;
            case REJECTED -> ApplicationStage.REJECTED;
        };
    }
}
