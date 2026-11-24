package com.portfolio.hireflow.interview;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.entity.Department;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.candidate.dto.CandidateRequest;
import com.portfolio.hireflow.candidate.dto.CandidateResponse;
import com.portfolio.hireflow.candidate.dto.CreateApplicationRequest;
import com.portfolio.hireflow.candidate.dto.TransitionApplicationRequest;
import com.portfolio.hireflow.candidate.entity.ApplicationStage;
import com.portfolio.hireflow.candidate.service.CandidateApplicationService;
import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.common.exception.ConflictException;
import com.portfolio.hireflow.common.exception.ForbiddenException;
import com.portfolio.hireflow.interview.dto.*;
import com.portfolio.hireflow.interview.entity.*;
import com.portfolio.hireflow.interview.service.InterviewService;
import com.portfolio.hireflow.requisition.entity.JobOpening;
import com.portfolio.hireflow.requisition.entity.JobRequisition;
import com.portfolio.hireflow.requisition.repository.JobOpeningRepository;
import com.portfolio.hireflow.requisition.repository.JobRequisitionRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InterviewIntegrationTests {

    private static final String HR_EMAIL = "hr@hireflow.local";
    private static final String INTERVIEWER_EMAIL = "interviewer@hireflow.local";
    private static final String MANAGER_EMAIL = "manager@hireflow.local";

    @Autowired InterviewService interviewService;
    @Autowired CandidateApplicationService candidateService;
    @Autowired AppUserRepository users;
    @Autowired JobRequisitionRepository requisitions;
    @Autowired JobOpeningRepository openings;

    @Test
    void rejectsPastAndInvalidTimeRanges() {
        Long applicationId = screeningApplication();
        Long interviewerId = user(INTERVIEWER_EMAIL).getId();
        Instant now = Instant.now();

        assertThatThrownBy(() -> interviewService.schedule(HR_EMAIL, schedule(
                applicationId, now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), interviewerId)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("晚於目前時間");

        assertThatThrownBy(() -> interviewService.schedule(HR_EMAIL, schedule(
                applicationId, now.plus(2, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS), interviewerId)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("結束時間");
    }

    @Test
    void rejectsOverlappingInterviewForSameInterviewer() {
        Long interviewerId = user(INTERVIEWER_EMAIL).getId();
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        interviewService.schedule(HR_EMAIL, schedule(
                screeningApplication(), start, start.plus(1, ChronoUnit.HOURS), interviewerId));

        assertThatThrownBy(() -> interviewService.schedule(HR_EMAIL, schedule(
                screeningApplication(), start.plus(30, ChronoUnit.MINUTES),
                start.plus(90, ChronoUnit.MINUTES), interviewerId)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("已有其他面試");
    }

    @Test
    void interviewerCannotViewInterviewThatWasNotAssigned() {
        InterviewResponse interview = interviewService.schedule(HR_EMAIL, schedule(
                screeningApplication(), Instant.now().plus(3, ChronoUnit.DAYS),
                Instant.now().plus(3, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS),
                user(MANAGER_EMAIL).getId()));

        assertThatThrownBy(() -> interviewService.findById(interview.id(), INTERVIEWER_EMAIL))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("指派給自己");
    }

    @Test
    void submittedScorecardIsLockedUntilHrUnlocksIt() {
        InterviewResponse interview = assignedInterview();
        ScorecardRequest submitted = scorecard(true);
        InterviewScorecardResponse result = interviewService.saveScorecard(
                interview.id(), INTERVIEWER_EMAIL, submitted);

        assertThat(result.status()).isEqualTo(ScorecardStatus.SUBMITTED);
        assertThatThrownBy(() -> interviewService.saveScorecard(interview.id(), INTERVIEWER_EMAIL, submitted))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("鎖定");

        InterviewScorecardResponse unlocked = interviewService.unlockScorecard(
                interview.id(), result.id(), HR_EMAIL, new UnlockScorecardRequest("面試官需更正內容"));
        assertThat(unlocked.status()).isEqualTo(ScorecardStatus.DRAFT);
        assertThat(unlocked.unlockReason()).isEqualTo("面試官需更正內容");
    }

    @Test
    void cancelledInterviewCannotBeScored() {
        InterviewResponse interview = assignedInterview();
        interviewService.cancel(interview.id(), HR_EMAIL, new CancelInterviewRequest("應徵者改期"));

        assertThatThrownBy(() -> interviewService.saveScorecard(
                interview.id(), INTERVIEWER_EMAIL, scorecard(false)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已取消");
    }

    @Test
    void completingScoredInterviewMovesApplicationToInterviewed() {
        InterviewResponse interview = assignedInterview();
        interviewService.saveScorecard(interview.id(), INTERVIEWER_EMAIL, scorecard(true));

        InterviewResponse completed = interviewService.complete(interview.id(), HR_EMAIL);
        var application = candidateService.findApplication(completed.applicationId(), HR_EMAIL);

        assertThat(completed.status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(application.stage()).isEqualTo(ApplicationStage.INTERVIEWED);
        assertThat(application.histories().get(application.histories().size() - 1).toStage())
                .isEqualTo(ApplicationStage.INTERVIEWED);
    }

    private InterviewResponse assignedInterview() {
        Instant start = Instant.now().plus(5, ChronoUnit.DAYS);
        return interviewService.schedule(HR_EMAIL, schedule(
                screeningApplication(), start, start.plus(1, ChronoUnit.HOURS),
                user(INTERVIEWER_EMAIL).getId()));
    }

    private ScheduleInterviewRequest schedule(Long applicationId, Instant start, Instant end, Long interviewerId) {
        return new ScheduleInterviewRequest(
                applicationId, 1, start, end, InterviewFormat.ONLINE, "Google Meet", Set.of(interviewerId));
    }

    private ScorecardRequest scorecard(boolean submit) {
        return new ScorecardRequest(4, 4, 5, 4, "技術基礎完整，溝通清楚。", ScorecardRecommendation.HIRE, submit);
    }

    private Long screeningApplication() {
        CandidateResponse candidate = candidateService.createCandidate(HR_EMAIL, new CandidateRequest(
                "測試人選", "candidate-" + UUID.randomUUID() + "@example.com", "0912-000-000",
                "Engineer", new BigDecimal("3.0"), "Java", null, null));
        JobOpening opening = opening();
        var application = candidateService.createApplication(
                HR_EMAIL, new CreateApplicationRequest(candidate.id(), opening.getId()));
        return candidateService.transition(application.id(), HR_EMAIL,
                new TransitionApplicationRequest(ApplicationStage.SCREENING, null)).id();
    }

    private JobOpening opening() {
        AppUser manager = user(MANAGER_EMAIL);
        AppUser hr = user(HR_EMAIL);
        Department department = manager.getDepartment();
        JobRequisition requisition = new JobRequisition(department, manager);
        requisition.update(department, "Software Engineer", 1, "擴充團隊", "Java",
                new BigDecimal("60000"), new BigDecimal("90000"), LocalDate.now().plusMonths(1));
        requisition.submit();
        requisition.approve();
        requisitions.save(requisition);
        return openings.saveAndFlush(new JobOpening(requisition, hr));
    }

    private AppUser user(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow();
    }
}
