package com.portfolio.hireflow.offer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.candidate.dto.*;
import com.portfolio.hireflow.candidate.entity.Application;
import com.portfolio.hireflow.candidate.entity.ApplicationStage;
import com.portfolio.hireflow.candidate.repository.ApplicationRepository;
import com.portfolio.hireflow.candidate.service.CandidateApplicationService;
import com.portfolio.hireflow.candidate.service.PublicCandidatePortalService;
import com.portfolio.hireflow.decision.dto.CreateDecisionRequest;
import com.portfolio.hireflow.decision.entity.DecisionResult;
import com.portfolio.hireflow.decision.service.HiringDecisionService;
import com.portfolio.hireflow.integration.email.repository.EmailDeliveryRepository;
import com.portfolio.hireflow.interview.dto.ScheduleInterviewRequest;
import com.portfolio.hireflow.interview.dto.ScorecardRequest;
import com.portfolio.hireflow.interview.entity.InterviewFormat;
import com.portfolio.hireflow.interview.entity.ScorecardRecommendation;
import com.portfolio.hireflow.interview.service.InterviewService;
import com.portfolio.hireflow.offer.dto.CreateJobOfferRequest;
import com.portfolio.hireflow.offer.dto.ReviewJobOfferRequest;
import com.portfolio.hireflow.offer.entity.JobOfferStatus;
import com.portfolio.hireflow.offer.entity.OfferReviewDecision;
import com.portfolio.hireflow.offer.service.CandidateAttachmentService;
import com.portfolio.hireflow.offer.service.JobOfferService;
import com.portfolio.hireflow.requisition.entity.JobOpening;
import com.portfolio.hireflow.requisition.entity.JobRequisition;
import com.portfolio.hireflow.requisition.repository.JobOpeningRepository;
import com.portfolio.hireflow.requisition.repository.JobRequisitionRepository;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.storage.resume-directory=target/e2e-resumes")
@Transactional
class RecruitmentFlowIntegrationTests {

    private static final String HR_EMAIL = "hr@hireflow.local";
    private static final String MANAGER_EMAIL = "manager@hireflow.local";
    private static final String INTERVIEWER_EMAIL = "interviewer@hireflow.local";
    private static final Path TEST_STORAGE = Path.of("target/e2e-resumes").toAbsolutePath().normalize();

    @Autowired CandidateApplicationService candidates;
    @Autowired InterviewService interviews;
    @Autowired HiringDecisionService decisions;
    @Autowired JobOfferService offers;
    @Autowired CandidateAttachmentService attachments;
    @Autowired PublicCandidatePortalService portal;
    @Autowired EmailDeliveryRepository emailDeliveries;
    @Autowired AppUserRepository users;
    @Autowired ApplicationRepository applications;
    @Autowired JobRequisitionRepository requisitions;
    @Autowired JobOpeningRepository openings;

    @AfterEach
    void removeStoredTestFiles() throws IOException {
        if (!Files.exists(TEST_STORAGE)) return;
        try (Stream<Path> paths = Files.walk(TEST_STORAGE)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new IllegalStateException("無法清除端對端測試履歷", exception);
                }
            });
        }
    }

    @Test
    void candidateCanMoveFromApplicationThroughInterviewToAcceptedOffer() {
        AppUser hr = user(HR_EMAIL);
        AppUser manager = user(MANAGER_EMAIL);
        AppUser interviewer = user(INTERVIEWER_EMAIL);
        JobOpening opening = opening(manager, hr);
        CandidateResponse candidate = candidates.createCandidate(HR_EMAIL, new CandidateRequest(
                "完整流程測試人選", "flow-" + UUID.randomUUID() + "@example.com", "0912-000-000",
                "Engineer", new BigDecimal("4.0"), "Java, Spring, Vue", null, null));
        ApplicationResponse application = candidates.createApplication(
                HR_EMAIL, new CreateApplicationRequest(candidate.id(), opening.getId()));
        candidates.transition(application.id(), HR_EMAIL,
                new TransitionApplicationRequest(ApplicationStage.SCREENING, null));

        Instant startsAt = Instant.now().plus(7, ChronoUnit.DAYS);
        var interview = interviews.schedule(HR_EMAIL, new ScheduleInterviewRequest(
                application.id(), 1, startsAt, startsAt.plus(1, ChronoUnit.HOURS),
                InterviewFormat.ONLINE, "Google Meet", Set.of(interviewer.getId())));
        interviews.saveScorecard(interview.id(), INTERVIEWER_EMAIL, new ScorecardRequest(
                4, 5, 4, 4, "技術與溝通能力符合職務需求。",
                ScorecardRecommendation.HIRE, true));
        interviews.complete(interview.id(), HR_EMAIL);
        candidates.transition(application.id(), HR_EMAIL,
                new TransitionApplicationRequest(ApplicationStage.DECISION_PENDING, null));

        var beforeDecision = decisions.findDecisionView(application.id(), MANAGER_EMAIL);
        var decisionView = decisions.decide(application.id(), MANAGER_EMAIL,
                new CreateDecisionRequest(DecisionResult.HIRED, "面試評分符合錄取標準",
                        beforeDecision.applicationVersion()));
        var draft = offers.create(HR_EMAIL, new CreateJobOfferRequest(
                application.id(), new BigDecimal("960000"), "TWD", LocalDate.now().plusDays(14),
                Instant.now().plus(7, ChronoUnit.DAYS), "錄取邀請",
                "恭喜通過完整招募流程，誠摯邀請你加入 HireFlow。"));
        offers.submit(draft.id(), HR_EMAIL);
        offers.review(draft.id(), MANAGER_EMAIL,
                new ReviewJobOfferRequest(OfferReviewDecision.APPROVE, null));
        var sent = offers.send(draft.id(), HR_EMAIL);

        Application entity = applications.findOneById(application.id()).orElseThrow();
        String accessPath = portal.issueAccessPath(entity);
        String token = accessPath.substring(accessPath.indexOf("token=") + 6);
        attachments.uploadPublic(entity.getPublicId(), token, new MockMultipartFile(
                "file", "candidate-resume.pdf", "application/pdf",
                "%PDF-1.7\nHireFlow integration resume".getBytes()));
        var accepted = offers.respond(entity.getPublicId(), token, true, null);

        assertThat(decisionView.stage()).isEqualTo(ApplicationStage.HIRED);
        assertThat(sent.status()).isEqualTo(JobOfferStatus.SENT);
        assertThat(accepted.status()).isEqualTo(JobOfferStatus.ACCEPTED);
        assertThat(attachments.findForApplication(application.id(), HR_EMAIL)).hasSize(1);
        assertThat(emailDeliveries.existsByDeduplicationKey("offer-sent-" + draft.id())).isTrue();
        assertThat(emailDeliveries.existsByDeduplicationKey(
                "offer-response-" + draft.id() + "-ACCEPTED")).isTrue();
    }

    private JobOpening opening(AppUser manager, AppUser hr) {
        JobRequisition requisition = new JobRequisition(manager.getDepartment(), manager);
        requisition.update(manager.getDepartment(), "Platform Engineer", 1, "擴充平台團隊",
                "Java, Spring, Vue", new BigDecimal("70000"), new BigDecimal("100000"),
                LocalDate.now().plusMonths(1));
        requisition.submit();
        requisition.approve();
        requisitions.save(requisition);
        return openings.saveAndFlush(new JobOpening(requisition, hr));
    }

    private AppUser user(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow();
    }
}
