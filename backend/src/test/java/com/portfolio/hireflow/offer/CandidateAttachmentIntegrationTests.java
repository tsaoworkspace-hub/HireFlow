package com.portfolio.hireflow.offer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
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
import com.portfolio.hireflow.auth.entity.Department;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.candidate.dto.CandidateRequest;
import com.portfolio.hireflow.candidate.dto.CandidateResponse;
import com.portfolio.hireflow.candidate.dto.CreateApplicationRequest;
import com.portfolio.hireflow.candidate.entity.Application;
import com.portfolio.hireflow.candidate.repository.ApplicationRepository;
import com.portfolio.hireflow.candidate.service.CandidateApplicationService;
import com.portfolio.hireflow.candidate.service.PublicCandidatePortalService;
import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.common.exception.ForbiddenException;
import com.portfolio.hireflow.offer.dto.CandidateAttachmentResponse;
import com.portfolio.hireflow.offer.entity.CandidateAttachment;
import com.portfolio.hireflow.offer.repository.CandidateAttachmentRepository;
import com.portfolio.hireflow.offer.service.CandidateAttachmentService;
import com.portfolio.hireflow.requisition.entity.JobOpening;
import com.portfolio.hireflow.requisition.entity.JobRequisition;
import com.portfolio.hireflow.requisition.repository.JobOpeningRepository;
import com.portfolio.hireflow.requisition.repository.JobRequisitionRepository;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.storage.resume-directory=target/test-resumes",
        "app.storage.resume-max-bytes=128"
})
@Transactional
class CandidateAttachmentIntegrationTests {

    private static final String HR_EMAIL = "hr@hireflow.local";
    private static final String MANAGER_EMAIL = "manager@hireflow.local";
    private static final Path TEST_STORAGE = Path.of("target/test-resumes").toAbsolutePath().normalize();

    @Autowired CandidateAttachmentService attachmentService;
    @Autowired CandidateAttachmentRepository attachments;
    @Autowired PublicCandidatePortalService portalService;
    @Autowired CandidateApplicationService candidateService;
    @Autowired ApplicationRepository applications;
    @Autowired AppUserRepository users;
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
                    throw new IllegalStateException("無法清除測試履歷", exception);
                }
            });
        }
    }

    @Test
    void newerPdfBecomesTheOnlyActiveResumeAndCanBeDownloaded() throws IOException {
        Application application = newApplication(user(MANAGER_EMAIL).getDepartment());
        String token = tokenFor(application);

        CandidateAttachmentResponse first = attachmentService.uploadPublic(
                application.getPublicId(), token, pdf("first.pdf", "%PDF-1.7\nfirst"));
        CandidateAttachmentResponse second = attachmentService.uploadPublic(
                application.getPublicId(), token, pdf("latest.pdf", "%PDF-1.7\nlatest"));

        assertThat(attachmentService.findPublic(application.getPublicId(), token))
                .extracting(CandidateAttachmentResponse::id)
                .containsExactly(second.id());
        assertThat(attachments.findById(first.id()).orElseThrow().isActive()).isFalse();
        var download = attachmentService.download(second.id(), HR_EMAIL);
        assertThat(download.originalFilename()).isEqualTo("latest.pdf");
        assertThat(download.resource().getInputStream().readAllBytes())
                .startsWith("%PDF-".getBytes());
    }

    @Test
    void uploadRejectsFakePdfAndFileOverConfiguredLimit() {
        Application application = newApplication(user(MANAGER_EMAIL).getDepartment());
        String token = tokenFor(application);

        assertThatThrownBy(() -> attachmentService.uploadPublic(
                application.getPublicId(), token,
                new MockMultipartFile("file", "fake.pdf", "application/pdf", "not-a-pdf".getBytes())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PDF");

        byte[] oversized = new byte[129];
        byte[] header = "%PDF-".getBytes();
        System.arraycopy(header, 0, oversized, 0, header.length);
        assertThatThrownBy(() -> attachmentService.uploadPublic(
                application.getPublicId(), token,
                new MockMultipartFile("file", "large.pdf", "application/pdf", oversized)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void managerCannotDownloadResumeFromAnotherDepartment() {
        Application application = newApplication(user(HR_EMAIL).getDepartment());
        CandidateAttachmentResponse uploaded = attachmentService.uploadPublic(
                application.getPublicId(), tokenFor(application), pdf("resume.pdf", "%PDF-1.7\nresume"));

        assertThatThrownBy(() -> attachmentService.download(uploaded.id(), MANAGER_EMAIL))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("自己部門");
    }

    @Test
    void storageKeyCannotEscapeConfiguredResumeDirectory() {
        Application application = newApplication(user(MANAGER_EMAIL).getDepartment());
        CandidateAttachment malicious = attachments.saveAndFlush(new CandidateAttachment(
                application, "resume.pdf", "../outside.pdf", "application/pdf", 10, "CANDIDATE"));

        assertThatThrownBy(() -> attachmentService.download(malicious.getId(), HR_EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("檔案路徑");
    }

    private MockMultipartFile pdf(String filename, String content) {
        return new MockMultipartFile("file", filename, "application/pdf", content.getBytes());
    }

    private String tokenFor(Application application) {
        String path = portalService.issueAccessPath(application);
        return path.substring(path.indexOf("token=") + 6);
    }

    private Application newApplication(Department department) {
        CandidateResponse candidate = candidateService.createCandidate(HR_EMAIL, new CandidateRequest(
                "附件測試人選", "attachment-" + UUID.randomUUID() + "@example.com", "0912-000-000",
                "Engineer", new BigDecimal("4.0"), "Java, Vue", null, null));
        var response = candidateService.createApplication(
                HR_EMAIL, new CreateApplicationRequest(candidate.id(), opening(department).getId()));
        return applications.findOneById(response.id()).orElseThrow();
    }

    private JobOpening opening(Department department) {
        AppUser manager = user(MANAGER_EMAIL);
        AppUser hr = user(HR_EMAIL);
        JobRequisition requisition = new JobRequisition(department, manager);
        requisition.update(department, "Attachment Test Engineer", 1, "附件測試", "Java",
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
