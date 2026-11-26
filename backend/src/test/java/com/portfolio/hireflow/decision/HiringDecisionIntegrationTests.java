package com.portfolio.hireflow.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.audit.entity.AuditAction;
import com.portfolio.hireflow.audit.repository.AuditLogRepository;
import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.entity.Department;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.candidate.dto.*;
import com.portfolio.hireflow.candidate.entity.ApplicationStage;
import com.portfolio.hireflow.candidate.repository.ApplicationRepository;
import com.portfolio.hireflow.candidate.service.CandidateApplicationService;
import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.common.exception.ConflictException;
import com.portfolio.hireflow.common.exception.ForbiddenException;
import com.portfolio.hireflow.decision.dto.CreateDecisionRequest;
import com.portfolio.hireflow.decision.dto.DecisionViewResponse;
import com.portfolio.hireflow.decision.entity.DecisionResult;
import com.portfolio.hireflow.decision.repository.HiringDecisionRepository;
import com.portfolio.hireflow.decision.service.HiringDecisionService;
import com.portfolio.hireflow.interview.dto.ScheduleInterviewRequest;
import com.portfolio.hireflow.interview.dto.ScorecardRequest;
import com.portfolio.hireflow.interview.entity.InterviewFormat;
import com.portfolio.hireflow.interview.entity.ScorecardRecommendation;
import com.portfolio.hireflow.interview.service.InterviewService;
import com.portfolio.hireflow.requisition.entity.JobOpening;
import com.portfolio.hireflow.requisition.entity.JobRequisition;
import com.portfolio.hireflow.requisition.repository.JobOpeningRepository;
import com.portfolio.hireflow.requisition.repository.JobRequisitionRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HiringDecisionIntegrationTests {

    private static final String HR_EMAIL = "hr@hireflow.local";
    private static final String MANAGER_EMAIL = "manager@hireflow.local";
    private static final String INTERVIEWER_EMAIL = "interviewer@hireflow.local";

    @Autowired HiringDecisionService decisionService;
    @Autowired CandidateApplicationService candidateService;
    @Autowired InterviewService interviewService;
    @Autowired ApplicationRepository applications;
    @Autowired HiringDecisionRepository decisions;
    @Autowired AuditLogRepository auditLogs;
    @Autowired AppUserRepository users;
    @Autowired JobRequisitionRepository requisitions;
    @Autowired JobOpeningRepository openings;

    @Test
    void applicationCannotEnterDecisionBeforeCompletedInterviewAndSubmittedScorecard() {
        ApplicationResponse application = newApplication(user(MANAGER_EMAIL).getDepartment());
        candidateService.transition(application.id(), HR_EMAIL,
                new TransitionApplicationRequest(ApplicationStage.SCREENING, null));
        var entity = applications.findOneById(application.id()).orElseThrow();
        entity.transitionTo(ApplicationStage.INTERVIEWED);
        applications.flush();

        assertThatThrownBy(() -> candidateService.transition(application.id(), HR_EMAIL,
                new TransitionApplicationRequest(ApplicationStage.DECISION_PENDING, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已完成的面試");
    }

    @Test
    void hrMayViewButCannotCreateFinalDecision() {
        ApplicationResponse application = readyApplication(user(MANAGER_EMAIL).getDepartment());

        DecisionViewResponse view = decisionService.findDecisionView(application.id(), HR_EMAIL);
        assertThat(view.readyForDecision()).isTrue();
        assertThat(view.canDecide()).isFalse();
        assertThatThrownBy(() -> decisionService.decide(application.id(), HR_EMAIL,
                new CreateDecisionRequest(DecisionResult.HIRED, "評分符合需求", view.applicationVersion())))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("用人主管");
    }

    @Test
    void managerCannotViewOrDecideForAnotherDepartment() {
        Department otherDepartment = user(HR_EMAIL).getDepartment();
        ApplicationResponse application = readyApplication(otherDepartment);

        assertThatThrownBy(() -> decisionService.findDecisionView(application.id(), MANAGER_EMAIL))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("自己部門");
    }

    @Test
    void decisionUpdatesApplicationHistoryAndAuditInOneUseCase() {
        ApplicationResponse application = readyApplication(user(MANAGER_EMAIL).getDepartment());
        DecisionViewResponse before = decisionService.findDecisionView(application.id(), MANAGER_EMAIL);

        DecisionViewResponse result = decisionService.decide(application.id(), MANAGER_EMAIL,
                new CreateDecisionRequest(DecisionResult.HIRED, "技術與團隊合作能力符合需求", before.applicationVersion()));

        assertThat(result.stage()).isEqualTo(ApplicationStage.HIRED);
        assertThat(result.decision()).isNotNull();
        assertThat(result.decision().result()).isEqualTo(DecisionResult.HIRED);
        assertThat(decisions.existsByApplicationId(application.id())).isTrue();
        assertThat(candidateService.findApplication(application.id(), HR_EMAIL).histories())
                .anySatisfy(history -> assertThat(history.toStage()).isEqualTo(ApplicationStage.HIRED));
        assertThat(auditLogs.findAll())
                .anySatisfy(log -> assertThat(log.getAction()).isEqualTo(AuditAction.HIRING_DECISION_CREATED));
    }

    @Test
    void sameApplicationCannotReceiveTwoFinalDecisions() {
        ApplicationResponse application = readyApplication(user(MANAGER_EMAIL).getDepartment());
        DecisionViewResponse before = decisionService.findDecisionView(application.id(), MANAGER_EMAIL);
        decisionService.decide(application.id(), MANAGER_EMAIL,
                new CreateDecisionRequest(DecisionResult.ON_HOLD, "等待最後人力確認", before.applicationVersion()));

        assertThatThrownBy(() -> decisionService.decide(application.id(), MANAGER_EMAIL,
                new CreateDecisionRequest(DecisionResult.HIRED, "重複決策", before.applicationVersion())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("已經完成");
    }

    @Test
    void decisionSearchIncludesFinalRejectionButExcludesScreeningRejection() {
        Department department = user(MANAGER_EMAIL).getDepartment();
        ApplicationResponse screeningRejection = newApplication(department);
        candidateService.transition(screeningRejection.id(), HR_EMAIL,
                new TransitionApplicationRequest(ApplicationStage.SCREENING, null));
        candidateService.transition(screeningRejection.id(), HR_EMAIL,
                new TransitionApplicationRequest(ApplicationStage.REJECTED, "初審未通過"));

        ApplicationResponse finalRejection = readyApplication(department);
        DecisionViewResponse before = decisionService.findDecisionView(finalRejection.id(), MANAGER_EMAIL);
        decisionService.decide(finalRejection.id(), MANAGER_EMAIL,
                new CreateDecisionRequest(DecisionResult.REJECTED, "面試評分未達要求", before.applicationVersion()));

        var results = candidateService.findApplications(HR_EMAIL, ApplicationStage.REJECTED,
                null, "決策測試人選", true, PageRequest.of(0, 50));
        assertThat(results.content()).extracting(ApplicationResponse::id)
                .contains(finalRejection.id())
                .doesNotContain(screeningRejection.id());
    }

    private ApplicationResponse readyApplication(Department department) {
        ApplicationResponse application = newApplication(department);
        candidateService.transition(application.id(), HR_EMAIL,
                new TransitionApplicationRequest(ApplicationStage.SCREENING, null));
        Instant startsAt = Instant.now().plus(7, ChronoUnit.DAYS);
        var interview = interviewService.schedule(HR_EMAIL, new ScheduleInterviewRequest(
                application.id(), 1, startsAt, startsAt.plus(1, ChronoUnit.HOURS),
                InterviewFormat.ONLINE, "Google Meet", Set.of(user(INTERVIEWER_EMAIL).getId())));
        interviewService.saveScorecard(interview.id(), INTERVIEWER_EMAIL, new ScorecardRequest(
                4, 5, 4, 4, "整體表現穩定，具備職務所需能力。", ScorecardRecommendation.HIRE, true));
        interviewService.complete(interview.id(), HR_EMAIL);
        return candidateService.transition(application.id(), HR_EMAIL,
                new TransitionApplicationRequest(ApplicationStage.DECISION_PENDING, null));
    }

    private ApplicationResponse newApplication(Department department) {
        CandidateResponse candidate = candidateService.createCandidate(HR_EMAIL, new CandidateRequest(
                "決策測試人選", "decision-" + UUID.randomUUID() + "@example.com", "0912-000-000",
                "Engineer", new BigDecimal("4.0"), "Java, Spring", null, null));
        JobOpening opening = opening(department);
        return candidateService.createApplication(
                HR_EMAIL, new CreateApplicationRequest(candidate.id(), opening.getId()));
    }

    private JobOpening opening(Department department) {
        AppUser manager = user(MANAGER_EMAIL);
        AppUser hr = user(HR_EMAIL);
        JobRequisition requisition = new JobRequisition(department, manager);
        requisition.update(department, "Platform Engineer", 1, "擴充團隊", "Java, Spring",
                new BigDecimal("70000"), new BigDecimal("100000"), LocalDate.now().plusMonths(1));
        requisition.submit();
        requisition.approve();
        requisitions.save(requisition);
        return openings.saveAndFlush(new JobOpening(requisition, hr));
    }

    private AppUser user(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow();
    }
}
