package com.portfolio.hireflow.offer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
import com.portfolio.hireflow.candidate.entity.Application;
import com.portfolio.hireflow.candidate.entity.ApplicationStage;
import com.portfolio.hireflow.candidate.repository.ApplicationRepository;
import com.portfolio.hireflow.candidate.service.CandidateApplicationService;
import com.portfolio.hireflow.candidate.service.PublicCandidatePortalService;
import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.common.exception.ForbiddenException;
import com.portfolio.hireflow.decision.entity.DecisionResult;
import com.portfolio.hireflow.decision.entity.HiringDecision;
import com.portfolio.hireflow.decision.repository.HiringDecisionRepository;
import com.portfolio.hireflow.integration.email.repository.EmailDeliveryRepository;
import com.portfolio.hireflow.offer.dto.CreateJobOfferRequest;
import com.portfolio.hireflow.offer.dto.JobOfferResponse;
import com.portfolio.hireflow.offer.dto.PublicJobOfferResponse;
import com.portfolio.hireflow.offer.dto.ReviewJobOfferRequest;
import com.portfolio.hireflow.offer.entity.JobOfferStatus;
import com.portfolio.hireflow.offer.entity.OfferReviewDecision;
import com.portfolio.hireflow.offer.service.JobOfferService;
import com.portfolio.hireflow.requisition.entity.JobOpening;
import com.portfolio.hireflow.requisition.entity.JobRequisition;
import com.portfolio.hireflow.requisition.repository.JobOpeningRepository;
import com.portfolio.hireflow.requisition.repository.JobRequisitionRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JobOfferIntegrationTests {

    private static final String HR_EMAIL = "hr@hireflow.local";
    private static final String MANAGER_EMAIL = "manager@hireflow.local";

    @Autowired JobOfferService offerService;
    @Autowired CandidateApplicationService candidateService;
    @Autowired PublicCandidatePortalService portalService;
    @Autowired AppUserRepository users;
    @Autowired ApplicationRepository applications;
    @Autowired HiringDecisionRepository decisions;
    @Autowired JobRequisitionRepository requisitions;
    @Autowired JobOpeningRepository openings;
    @Autowired EmailDeliveryRepository emails;

    @Test
    void onlyHiredApplicationMayReceiveOffer() {
        Application application = newApplication(user(MANAGER_EMAIL).getDepartment());

        assertThatThrownBy(() -> offerService.create(HR_EMAIL, request(application.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已完成錄取決策");
    }

    @Test
    void hrAndDepartmentManagerCanCompleteOfferApprovalAndSend() {
        Application application = hiredApplication(user(MANAGER_EMAIL).getDepartment());

        JobOfferResponse draft = offerService.create(HR_EMAIL, request(application.getId()));
        JobOfferResponse pending = offerService.submit(draft.id(), HR_EMAIL);
        JobOfferResponse approved = offerService.review(pending.id(), MANAGER_EMAIL,
                new ReviewJobOfferRequest(OfferReviewDecision.APPROVE, null));
        JobOfferResponse sent = offerService.send(approved.id(), HR_EMAIL);

        assertThat(draft.status()).isEqualTo(JobOfferStatus.DRAFT);
        assertThat(pending.status()).isEqualTo(JobOfferStatus.PENDING_APPROVAL);
        assertThat(approved.status()).isEqualTo(JobOfferStatus.APPROVED);
        assertThat(sent.status()).isEqualTo(JobOfferStatus.SENT);
        assertThat(sent.histories()).hasSize(4);
        assertThat(emails.existsByDeduplicationKey("offer-sent-" + sent.id())).isTrue();
    }

    @Test
    void managerCannotApproveOfferFromAnotherDepartment() {
        Application application = hiredApplication(user(HR_EMAIL).getDepartment());
        JobOfferResponse draft = offerService.create(HR_EMAIL, request(application.getId()));
        JobOfferResponse pending = offerService.submit(draft.id(), HR_EMAIL);

        assertThatThrownBy(() -> offerService.review(pending.id(), MANAGER_EMAIL,
                new ReviewJobOfferRequest(OfferReviewDecision.APPROVE, null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("該部門");
    }

    @Test
    void candidateMayDeclineSentOfferThroughMagicLink() {
        Application application = hiredApplication(user(MANAGER_EMAIL).getDepartment());
        JobOfferResponse draft = offerService.create(HR_EMAIL, request(application.getId()));
        offerService.submit(draft.id(), HR_EMAIL);
        offerService.review(draft.id(), MANAGER_EMAIL,
                new ReviewJobOfferRequest(OfferReviewDecision.APPROVE, null));
        offerService.send(draft.id(), HR_EMAIL);
        String accessPath = portalService.issueAccessPath(application);
        String token = accessPath.substring(accessPath.indexOf("token=") + 6);

        PublicJobOfferResponse response = offerService.respond(
                application.getPublicId(), token, false, "接受其他職涯規劃");

        assertThat(response.status()).isEqualTo(JobOfferStatus.DECLINED);
        assertThat(emails.existsByDeduplicationKey("offer-response-" + draft.id() + "-DECLINED"))
                .isTrue();
    }

    private CreateJobOfferRequest request(Long applicationId) {
        return new CreateJobOfferRequest(
                applicationId, new BigDecimal("1200000"), "TWD",
                LocalDate.now().plusDays(14), Instant.now().plus(7, ChronoUnit.DAYS),
                "錄取邀請", "誠摯邀請你加入 HireFlow 團隊。");
    }

    private Application hiredApplication(Department department) {
        Application application = newApplication(department);
        application.transitionTo(ApplicationStage.HIRED);
        decisions.save(new HiringDecision(
                application, DecisionResult.HIRED, "符合職務需求", user(MANAGER_EMAIL)));
        return applications.saveAndFlush(application);
    }

    private Application newApplication(Department department) {
        CandidateResponse candidate = candidateService.createCandidate(HR_EMAIL, new CandidateRequest(
                "Offer 測試人選", "offer-" + UUID.randomUUID() + "@example.com", "0912-000-000",
                "Engineer", new BigDecimal("4.0"), "Java, Vue", null, null));
        var application = candidateService.createApplication(
                HR_EMAIL, new CreateApplicationRequest(candidate.id(), opening(department).getId()));
        return applications.findOneById(application.id()).orElseThrow();
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
