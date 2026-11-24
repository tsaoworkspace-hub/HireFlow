package com.portfolio.hireflow.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.entity.Department;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.candidate.dto.ApplicationResponse;
import com.portfolio.hireflow.candidate.dto.CandidateRequest;
import com.portfolio.hireflow.candidate.dto.CandidateResponse;
import com.portfolio.hireflow.candidate.dto.CreateApplicationRequest;
import com.portfolio.hireflow.candidate.dto.TransitionApplicationRequest;
import com.portfolio.hireflow.candidate.entity.ApplicationStage;
import com.portfolio.hireflow.candidate.service.CandidateApplicationService;
import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.common.exception.ConflictException;
import com.portfolio.hireflow.common.exception.ForbiddenException;
import com.portfolio.hireflow.requisition.entity.JobOpening;
import com.portfolio.hireflow.requisition.entity.JobRequisition;
import com.portfolio.hireflow.requisition.repository.JobOpeningRepository;
import com.portfolio.hireflow.requisition.repository.JobRequisitionRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CandidateApplicationIntegrationTests {

    private static final String HR_EMAIL = "hr@hireflow.local";
    private static final String MANAGER_EMAIL = "manager@hireflow.local";

    @Autowired CandidateApplicationService service;
    @Autowired AppUserRepository users;
    @Autowired JobRequisitionRepository requisitions;
    @Autowired JobOpeningRepository openings;

    @Test
    void duplicateCandidateEmailIsRejectedIgnoringCase() {
        String email = uniqueEmail();
        service.createCandidate(HR_EMAIL, candidateRequest("王小明", email));

        assertThatThrownBy(() -> service.createCandidate(HR_EMAIL, candidateRequest("另一位", email.toUpperCase())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("已有應徵者");
    }

    @Test
    void sameCandidateMayApplyToDifferentOpeningsButNotSameOpeningTwice() {
        CandidateResponse candidate = service.createCandidate(HR_EMAIL, candidateRequest("林小華", uniqueEmail()));
        JobOpening first = opening("Backend Engineer", "Engineering");
        JobOpening second = opening("Frontend Engineer", "Engineering");

        service.createApplication(HR_EMAIL, new CreateApplicationRequest(candidate.id(), first.getId()));
        service.createApplication(HR_EMAIL, new CreateApplicationRequest(candidate.id(), second.getId()));

        assertThatThrownBy(() -> service.createApplication(
                HR_EMAIL, new CreateApplicationRequest(candidate.id(), first.getId())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("已經應徵過");
    }

    @Test
    void closedOpeningCannotAcceptNewApplication() {
        CandidateResponse candidate = service.createCandidate(HR_EMAIL, candidateRequest("陳大同", uniqueEmail()));
        JobOpening opening = opening("QA Engineer", "Engineering");
        opening.close();
        openings.flush();

        assertThatThrownBy(() -> service.createApplication(
                HR_EMAIL, new CreateApplicationRequest(candidate.id(), opening.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已關閉");
    }

    @Test
    void stageTransitionCreatesHistoryAndRejectsIllegalJump() {
        CandidateResponse candidate = service.createCandidate(HR_EMAIL, candidateRequest("李美玲", uniqueEmail()));
        ApplicationResponse application = service.createApplication(
                HR_EMAIL, new CreateApplicationRequest(candidate.id(), opening("Data Engineer", "Engineering").getId()));

        ApplicationResponse screening = service.transition(
                application.id(), HR_EMAIL, new TransitionApplicationRequest(ApplicationStage.SCREENING, null));

        assertThat(screening.stage()).isEqualTo(ApplicationStage.SCREENING);
        assertThat(screening.histories()).hasSize(2);
        assertThat(screening.histories().get(1).fromStage()).isEqualTo(ApplicationStage.NEW);
        assertThatThrownBy(() -> service.transition(
                application.id(), HR_EMAIL, new TransitionApplicationRequest(ApplicationStage.HIRED, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允許");
    }

    @Test
    void managerCannotAccessApplicationOutsideOwnDepartment() {
        CandidateResponse candidate = service.createCandidate(HR_EMAIL, candidateRequest("周小玉", uniqueEmail()));
        ApplicationResponse application = service.createApplication(
                HR_EMAIL, new CreateApplicationRequest(candidate.id(), opening("HR Specialist", "Human Resources").getId()));

        assertThatThrownBy(() -> service.findApplication(application.id(), MANAGER_EMAIL))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("自己部門");
    }

    private CandidateRequest candidateRequest(String name, String email) {
        return new CandidateRequest(name, email, "0912-345-678", "Engineer",
                new BigDecimal("3.5"), "Java, Vue", null, null);
    }

    private String uniqueEmail() {
        return "candidate-" + UUID.randomUUID() + "@example.com";
    }

    private JobOpening opening(String title, String departmentName) {
        AppUser hr = users.findByEmailIgnoreCase(HR_EMAIL).orElseThrow();
        AppUser departmentUser = departmentName.equals("Engineering")
                ? users.findByEmailIgnoreCase(MANAGER_EMAIL).orElseThrow()
                : hr;
        Department department = departmentUser.getDepartment();
        JobRequisition requisition = new JobRequisition(department, departmentUser);
        requisition.update(department, title, 1, "擴充團隊", "Java", new BigDecimal("50000"),
                new BigDecimal("80000"), LocalDate.now().plusMonths(1));
        requisition.submit();
        requisition.approve();
        requisitions.save(requisition);
        return openings.saveAndFlush(new JobOpening(requisition, hr));
    }
}
