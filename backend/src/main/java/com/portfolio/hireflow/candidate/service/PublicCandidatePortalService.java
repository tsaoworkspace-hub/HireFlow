package com.portfolio.hireflow.candidate.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.candidate.dto.*;
import com.portfolio.hireflow.candidate.entity.*;
import com.portfolio.hireflow.candidate.repository.*;
import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.common.exception.ConflictException;
import com.portfolio.hireflow.common.exception.ResourceNotFoundException;
import com.portfolio.hireflow.requisition.entity.JobOpening;
import com.portfolio.hireflow.requisition.entity.JobOpeningStatus;
import com.portfolio.hireflow.requisition.repository.JobOpeningRepository;
import com.portfolio.hireflow.integration.email.entity.EmailMessageType;
import com.portfolio.hireflow.integration.email.service.EmailOutboxService;

@Service
public class PublicCandidatePortalService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JobOpeningRepository openings;
    private final CandidateRepository candidates;
    private final CandidateProfileRepository profiles;
    private final WorkExperienceRepository experiences;
    private final ApplicationRepository applications;
    private final ApplicationStageHistoryRepository histories;
    private final ApplicantAccessTokenRepository tokens;
    private final long tokenTtlDays;
    private final EmailOutboxService emailOutbox;
    private final String frontendBaseUrl;

    public PublicCandidatePortalService(JobOpeningRepository openings, CandidateRepository candidates,
            CandidateProfileRepository profiles, WorkExperienceRepository experiences,
            ApplicationRepository applications, ApplicationStageHistoryRepository histories,
            ApplicantAccessTokenRepository tokens,
            EmailOutboxService emailOutbox,
            @Value("${app.candidate-portal.token-ttl-days:30}") long tokenTtlDays,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.openings = openings; this.candidates = candidates; this.profiles = profiles;
        this.experiences = experiences; this.applications = applications; this.histories = histories;
        this.tokens = tokens; this.tokenTtlDays = tokenTtlDays;
        this.emailOutbox = emailOutbox; this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional(readOnly = true)
    public List<PublicJobResponse> findPublishedJobs() {
        return openings.findByStatusAndPublishedAtIsNotNullOrderByPublishedAtDesc(JobOpeningStatus.OPEN)
                .stream().map(this::toJob).toList();
    }

    @Transactional(readOnly = true)
    public PublicJobResponse findPublishedJob(String slug) { return toJob(requireOpening(slug)); }

    @Transactional
    public PublicApplicationCreatedResponse apply(String slug, PublicApplicationRequest request) {
        JobOpening opening = requireOpening(slug);
        Candidate candidate = candidates.findByEmailIgnoreCase(request.email()).orElseGet(
                () -> candidates.save(new Candidate(request.fullName(), request.email())));

        if (applications.existsByCandidateIdAndJobOpeningId(candidate.getId(), opening.getId())) {
            throw new ConflictException("這個 Email 已經應徵過此職缺");
        }

        candidate.update(request.fullName(), request.email(), request.phone(), request.currentPosition(),
                request.yearsExperience(), request.skills(), candidate.getNotes());

        CandidateProfile profile = profiles.findByCandidateId(candidate.getId())
                .orElseGet(() -> new CandidateProfile(candidate));
        profile.update(request.address(), request.autobiography(), request.portfolioUrl(), Instant.now());
        profiles.save(profile);

        experiences.deleteByCandidateId(candidate.getId());
        List<PublicApplicationRequest.WorkExperienceRequest> work = request.workExperiences() == null
                ? List.of() : request.workExperiences();
        for (int i = 0; i < work.size(); i++) {
            var item = work.get(i);
            if (!item.currentJob() && item.startDate() != null && item.endDate() != null
                    && item.endDate().isBefore(item.startDate())) {
                throw new BusinessException("工作經歷的結束日期不可早於開始日期");
            }
            experiences.save(new WorkExperience(candidate, item.companyName(), item.jobTitle(), item.startDate(),
                    item.endDate(), item.currentJob(), item.description(), i));
        }

        Application application = applications.save(new Application(candidate, opening, opening.getResponsibleHr()));
        histories.save(new ApplicationStageHistory(application, null, ApplicationStage.NEW,
                "求職者由公開招募頁送出履歷", opening.getResponsibleHr()));

        String rawToken = newToken();
        tokens.save(new ApplicantAccessToken(application, hash(rawToken),
                Instant.now().plus(tokenTtlDays, ChronoUnit.DAYS)));
        String path = "/candidate/applications/" + application.getPublicId() + "?token=" + rawToken;
        emailOutbox.queue("application-received-" + application.getId(), EmailMessageType.APPLICATION_RECEIVED,
                "APPLICATION", application.getId(), candidate.getEmail(), "HireFlow｜已收到你的應徵資料",
                candidate.getFullName() + " 您好：\n\n我們已收到你應徵「" + opening.getJobTitle()
                        + "」的資料。你可以使用下列專屬連結查看進度與選擇面試時間：\n"
                        + frontendBaseUrl.replaceAll("/+$", "") + path + "\n\n此連結請勿轉寄他人。");
        return new PublicApplicationCreatedResponse(application.getPublicId(), rawToken, path);
    }

    @Transactional
    public CandidatePortalResponse findApplication(UUID publicId, String rawToken) {
        Application application = requireAccessibleApplication(publicId, rawToken);
        List<CandidatePortalResponse.TimelineItem> timeline = histories
                .findByApplicationIdOrderByCreatedAtAscIdAsc(application.getId()).stream()
                .map(history -> new CandidatePortalResponse.TimelineItem(
                        publicStatus(history.getToStage()), statusLabel(history.getToStage()), history.getCreatedAt()))
                .toList();
        return new CandidatePortalResponse(application.getPublicId(), application.getCandidate().getFullName(),
                application.getJobOpening().getJobTitle(), application.getJobOpening().getDepartment().getName(),
                publicStatus(application.getStage()), statusLabel(application.getStage()), application.getCreatedAt(), timeline);
    }

    @Transactional
    public Application requireAccessibleApplication(UUID publicId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new BusinessException("缺少查詢憑證");
        ApplicantAccessToken token = tokens.findByTokenHash(hash(rawToken))
                .filter(value -> value.getApplication().getPublicId().equals(publicId))
                .orElseThrow(() -> new ResourceNotFoundException("找不到應徵紀錄或查詢連結無效"));
        if (!token.isUsableAt(Instant.now())) throw new BusinessException("查詢連結已過期，請聯絡招募窗口");
        token.markAccessed();
        return applications.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到應徵紀錄"));
    }

    @Transactional
    public String issueAccessPath(Application application) {
        String rawToken = newToken();
        tokens.save(new ApplicantAccessToken(application, hash(rawToken),
                Instant.now().plus(tokenTtlDays, ChronoUnit.DAYS)));
        return "/candidate/applications/" + application.getPublicId() + "?token=" + rawToken;
    }

    private JobOpening requireOpening(String slug) {
        return openings.findByPublicSlugAndStatusAndPublishedAtIsNotNull(slug, JobOpeningStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("找不到公開職缺"));
    }

    private PublicJobResponse toJob(JobOpening opening) {
        var requisition = opening.getRequisition();
        return new PublicJobResponse(opening.getPublicSlug(), opening.getJobTitle(),
                opening.getDepartment().getName(), opening.getLocation(), opening.getEmploymentType(),
                opening.getPublicDescription(), requisition.getRequiredSkills(), requisition.getSalaryMin(),
                requisition.getSalaryMax(), opening.getPublishedAt());
    }

    private static String publicStatus(ApplicationStage stage) {
        return switch (stage) {
            case NEW -> "SUBMITTED";
            case SCREENING -> "UNDER_REVIEW";
            case INTERVIEW_SCHEDULED -> "INTERVIEW_CONFIRMED";
            case INTERVIEWED -> "INTERVIEW_COMPLETED";
            case DECISION_PENDING, ON_HOLD -> "RESULT_PENDING";
            case HIRED -> "HIRED";
            case REJECTED -> "NOT_SELECTED";
            case WITHDRAWN -> "WITHDRAWN";
        };
    }

    private static String statusLabel(ApplicationStage stage) {
        return switch (stage) {
            case NEW -> "已收到履歷";
            case SCREENING -> "公司已查看履歷（初審中）";
            case INTERVIEW_SCHEDULED -> "面試時間已確認";
            case INTERVIEWED -> "面試已完成";
            case DECISION_PENDING, ON_HOLD -> "結果確認中";
            case HIRED -> "錄取";
            case REJECTED -> "本次未錄取";
            case WITHDRAWN -> "已撤回應徵";
        };
    }

    private static String newToken() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
