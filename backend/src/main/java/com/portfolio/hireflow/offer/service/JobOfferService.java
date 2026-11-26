package com.portfolio.hireflow.offer.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.audit.entity.AuditAction;
import com.portfolio.hireflow.audit.entity.AuditTargetType;
import com.portfolio.hireflow.audit.service.AuditLogService;
import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.candidate.entity.Application;
import com.portfolio.hireflow.candidate.entity.ApplicationStage;
import com.portfolio.hireflow.candidate.repository.ApplicationRepository;
import com.portfolio.hireflow.candidate.service.PublicCandidatePortalService;
import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.common.exception.ConflictException;
import com.portfolio.hireflow.common.exception.ForbiddenException;
import com.portfolio.hireflow.common.exception.ResourceNotFoundException;
import com.portfolio.hireflow.common.exception.UnauthorizedException;
import com.portfolio.hireflow.common.response.PageResponse;
import com.portfolio.hireflow.decision.entity.DecisionResult;
import com.portfolio.hireflow.decision.repository.HiringDecisionRepository;
import com.portfolio.hireflow.integration.email.entity.EmailMessageType;
import com.portfolio.hireflow.integration.email.service.EmailOutboxService;
import com.portfolio.hireflow.offer.dto.*;
import com.portfolio.hireflow.offer.entity.*;
import com.portfolio.hireflow.offer.repository.JobOfferHistoryRepository;
import com.portfolio.hireflow.offer.repository.JobOfferRepository;

@Service
@Transactional(readOnly = true)
public class JobOfferService {

    private static final Set<JobOfferStatus> CLOSED_STATUSES = Set.of(
            JobOfferStatus.ACCEPTED, JobOfferStatus.DECLINED,
            JobOfferStatus.EXPIRED, JobOfferStatus.CANCELLED);
    private static final Set<JobOfferStatus> PUBLIC_STATUSES = Set.of(
            JobOfferStatus.SENT, JobOfferStatus.ACCEPTED,
            JobOfferStatus.DECLINED, JobOfferStatus.EXPIRED);

    private final JobOfferRepository offers;
    private final JobOfferHistoryRepository histories;
    private final ApplicationRepository applications;
    private final HiringDecisionRepository decisions;
    private final AppUserRepository users;
    private final PublicCandidatePortalService portal;
    private final EmailOutboxService email;
    private final AuditLogService audit;
    private final String frontendBaseUrl;

    public JobOfferService(
            JobOfferRepository offers,
            JobOfferHistoryRepository histories,
            ApplicationRepository applications,
            HiringDecisionRepository decisions,
            AppUserRepository users,
            PublicCandidatePortalService portal,
            EmailOutboxService email,
            AuditLogService audit,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.offers = offers;
        this.histories = histories;
        this.applications = applications;
        this.decisions = decisions;
        this.users = users;
        this.portal = portal;
        this.email = email;
        this.audit = audit;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public PageResponse<JobOfferResponse> findAll(String emailAddress, String keyword,
            JobOfferStatus status, Pageable pageable) {
        AppUser actor = actor(emailAddress);
        Specification<JobOffer> scope = (root, query, builder) -> allDepartments(actor)
                ? builder.conjunction()
                : builder.equal(root.get("application").get("jobOpening").get("department").get("id"), departmentId(actor));
        if (keyword != null && !keyword.isBlank()) {
            String term = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            scope = scope.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("application").get("candidate").get("fullName")), term),
                    builder.like(builder.lower(root.get("application").get("candidate").get("email")), term),
                    builder.like(builder.lower(root.get("application").get("jobOpening").get("jobTitle")), term)));
        }
        if (status != null) {
            scope = scope.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        Page<JobOffer> page = offers.findAll(scope, pageable);
        return PageResponse.from(page.map(offer -> response(offer, actor, false)));
    }

    public List<OfferOptionResponse> options(String emailAddress) {
        AppUser actor = actor(emailAddress);
        requireHr(actor);
        Specification<Application> hired = (root, query, builder) ->
                builder.equal(root.get("stage"), ApplicationStage.HIRED);
        return applications.findAll(hired,
                        PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "updatedAt")))
                .stream()
                .filter(application -> !offers.existsByApplicationId(application.getId()))
                .map(application -> new OfferOptionResponse(
                        application.getId(), application.getCandidate().getFullName(),
                        application.getCandidate().getEmail(),
                        application.getJobOpening().getJobTitle(),
                        application.getJobOpening().getDepartment().getName()))
                .toList();
    }

    public JobOfferResponse find(Long id, String emailAddress) {
        AppUser actor = actor(emailAddress);
        JobOffer offer = offer(id);
        ensureView(actor, offer);
        return response(offer, actor, true);
    }

    @Transactional
    public JobOfferResponse create(String emailAddress, CreateJobOfferRequest request) {
        AppUser actor = actor(emailAddress);
        requireHr(actor);
        Application application = application(request.applicationId());
        boolean isHired = application.getStage() == ApplicationStage.HIRED
                && decisions.findByApplicationId(application.getId())
                        .filter(decision -> decision.getResult() == DecisionResult.HIRED)
                        .isPresent();
        if (!isHired) throw new BusinessException("只有已完成錄取決策的人選可以建立 Offer");
        if (offers.existsByApplicationId(application.getId())) {
            throw new ConflictException("此應徵紀錄已經有 Offer");
        }

        validateDates(request.proposedStartDate(), request.expiresAt());
        JobOffer offer = offers.save(new JobOffer(
                application, request.annualSalary(), request.currency(), request.proposedStartDate(),
                request.expiresAt(), request.title(), request.message(), actor));
        history(offer, null, JobOfferStatus.DRAFT, "建立 Offer 草稿",
                actor, OfferActorType.COMPANY, actor.getDisplayName());
        audit.record(actor, AuditAction.JOB_OFFER_CREATED, AuditTargetType.JOB_OFFER,
                offer.getId(), "Application #" + application.getId());
        return response(offer, actor, true);
    }

    @Transactional
    public JobOfferResponse update(Long id, String emailAddress, UpdateJobOfferRequest request) {
        AppUser actor = actor(emailAddress);
        requireHr(actor);
        JobOffer offer = offer(id);
        if (offer.getStatus() != JobOfferStatus.DRAFT
                && offer.getStatus() != JobOfferStatus.RETURNED) {
            throw new BusinessException("只有草稿或退回的 Offer 可以修改");
        }
        if (offer.getVersion() != request.version()) {
            throw new ConflictException("Offer 已被更新，請重新整理");
        }
        validateDates(request.proposedStartDate(), request.expiresAt());
        offer.update(request.annualSalary(), request.currency(), request.proposedStartDate(),
                request.expiresAt(), request.title(), request.message());
        audit.record(actor, AuditAction.JOB_OFFER_UPDATED,
                AuditTargetType.JOB_OFFER, offer.getId(), null);
        return response(offer, actor, true);
    }

    @Transactional
    public JobOfferResponse submit(Long id, String emailAddress) {
        AppUser actor = actor(emailAddress);
        requireHr(actor);
        JobOffer offer = offer(id);
        if (offer.getStatus() != JobOfferStatus.DRAFT
                && offer.getStatus() != JobOfferStatus.RETURNED) {
            throw new BusinessException("只有草稿或退回的 Offer 可以送審");
        }
        JobOfferStatus from = offer.getStatus();
        offer.submit();
        history(offer, from, offer.getStatus(), null,
                actor, OfferActorType.COMPANY, actor.getDisplayName());
        audit.record(actor, AuditAction.JOB_OFFER_SUBMITTED,
                AuditTargetType.JOB_OFFER, id, null);
        return response(offer, actor, true);
    }

    @Transactional
    public JobOfferResponse review(Long id, String emailAddress, ReviewJobOfferRequest request) {
        AppUser actor = actor(emailAddress);
        JobOffer offer = offer(id);
        requireManagerFor(actor, offer);
        if (offer.getStatus() != JobOfferStatus.PENDING_APPROVAL) {
            throw new BusinessException("只有待核准 Offer 可以審核");
        }
        JobOfferStatus from = offer.getStatus();
        if (request.decision() == OfferReviewDecision.APPROVE) {
            offer.approve(actor);
            history(offer, from, offer.getStatus(), request.reason(),
                    actor, OfferActorType.COMPANY, actor.getDisplayName());
            audit.record(actor, AuditAction.JOB_OFFER_APPROVED,
                    AuditTargetType.JOB_OFFER, id, null);
        } else {
            if (request.reason() == null || request.reason().isBlank()) {
                throw new BusinessException("退回理由為必填");
            }
            offer.returnForRevision();
            history(offer, from, offer.getStatus(), request.reason(),
                    actor, OfferActorType.COMPANY, actor.getDisplayName());
            audit.record(actor, AuditAction.JOB_OFFER_RETURNED,
                    AuditTargetType.JOB_OFFER, id, request.reason());
        }
        return response(offer, actor, true);
    }

    @Transactional
    public JobOfferResponse send(Long id, String emailAddress) {
        AppUser actor = actor(emailAddress);
        requireHr(actor);
        JobOffer offer = offer(id);
        if (offer.getStatus() != JobOfferStatus.APPROVED) {
            throw new BusinessException("只有已核准 Offer 可以寄出");
        }
        if (!offer.getExpiresAt().isAfter(Instant.now())) {
            throw new BusinessException("Offer 有效期限已過");
        }
        JobOfferStatus from = offer.getStatus();
        offer.send();
        history(offer, from, offer.getStatus(), null,
                actor, OfferActorType.COMPANY, actor.getDisplayName());
        String link = frontendBaseUrl.replaceAll("/+$", "")
                + portal.issueAccessPath(offer.getApplication());
        email.queue("offer-sent-" + offer.getId(), EmailMessageType.OFFER_SENT,
                "JOB_OFFER", offer.getId(), offer.getApplication().getCandidate().getEmail(),
                "HireFlow｜錄取邀請",
                offer.getApplication().getCandidate().getFullName() + " 您好：\n\n"
                        + offer.getMessage() + "\n\n請在 " + offer.getExpiresAt()
                        + " 前查看並回覆：\n" + link + "\n\n此連結請勿轉寄。");
        audit.record(actor, AuditAction.JOB_OFFER_SENT,
                AuditTargetType.JOB_OFFER, id, null);
        return response(offer, actor, true);
    }

    @Transactional
    public JobOfferResponse cancel(Long id, String emailAddress, String reason) {
        AppUser actor = actor(emailAddress);
        requireHr(actor);
        JobOffer offer = offer(id);
        if (CLOSED_STATUSES.contains(offer.getStatus())) {
            throw new BusinessException("此 Offer 已結案");
        }
        JobOfferStatus from = offer.getStatus();
        offer.cancel(reason);
        history(offer, from, offer.getStatus(), reason,
                actor, OfferActorType.COMPANY, actor.getDisplayName());
        audit.record(actor, AuditAction.JOB_OFFER_CANCELLED,
                AuditTargetType.JOB_OFFER, id, reason);
        return response(offer, actor, true);
    }

    public PublicJobOfferResponse publicOffer(UUID publicId, String token) {
        Application application = portal.requireAccessibleApplication(publicId, token);
        JobOffer offer = offers.findByApplicationId(application.getId())
                .filter(item -> PUBLIC_STATUSES.contains(item.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("目前沒有可查看的 Offer"));
        return publicResponse(offer);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public PublicJobOfferResponse respond(UUID publicId, String token, boolean accept, String reason) {
        Application application = portal.requireAccessibleApplication(publicId, token);
        JobOffer offer = offers.findByApplicationId(application.getId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到 Offer"));
        if (offer.getStatus() != JobOfferStatus.SENT) {
            throw new BusinessException("此 Offer 目前無法回覆");
        }
        if (!offer.getExpiresAt().isAfter(Instant.now())) {
            expire(offer);
            throw new BusinessException("Offer 已過期");
        }
        if (!accept && (reason == null || reason.isBlank())) {
            throw new BusinessException("拒絕 Offer 時請填寫原因");
        }
        JobOfferStatus from = offer.getStatus();
        if (accept) offer.accept(); else offer.decline(reason);
        String candidateName = application.getCandidate().getFullName();
        history(offer, from, offer.getStatus(), reason,
                null, OfferActorType.CANDIDATE, candidateName);
        EmailMessageType type = accept
                ? EmailMessageType.OFFER_ACCEPTED : EmailMessageType.OFFER_DECLINED;
        email.queue("offer-response-" + offer.getId() + "-" + offer.getStatus(), type,
                "JOB_OFFER", offer.getId(), offer.getCreatedBy().getEmail(),
                "HireFlow｜求職者已回覆 Offer",
                candidateName + " 已" + (accept ? "接受" : "拒絕") + "「"
                        + application.getJobOpening().getJobTitle() + "」Offer。"
                        + (reason == null ? "" : "\n原因：" + reason));
        audit.record(offer.getCreatedBy(),
                accept ? AuditAction.JOB_OFFER_ACCEPTED : AuditAction.JOB_OFFER_DECLINED,
                AuditTargetType.JOB_OFFER, offer.getId(), "Candidate portal response");
        return publicResponse(offer);
    }

    @Transactional
    public void expire(JobOffer offer) {
        if (offer.getStatus() != JobOfferStatus.SENT) return;
        JobOfferStatus from = offer.getStatus();
        offer.expire();
        history(offer, from, offer.getStatus(), "超過有效期限",
                null, OfferActorType.SYSTEM, "系統");
    }

    private void validateDates(LocalDate startDate, Instant expiresAt) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessException("到職日不可早於今天");
        }
        if (!expiresAt.isAfter(Instant.now())) {
            throw new BusinessException("Offer 有效期限必須晚於目前時間");
        }
    }

    private JobOfferResponse response(JobOffer offer, AppUser actor, boolean details) {
        boolean hr = allDepartments(actor);
        boolean manager = has(actor, "HIRING_MANAGER") || has(actor, "ADMIN");
        List<JobOfferHistoryResponse> offerHistories = details
                ? histories.findByOfferIdOrderByCreatedAtAscIdAsc(offer.getId()).stream()
                        .map(history -> new JobOfferHistoryResponse(
                                history.getId(), history.getFromStatus(), history.getToStatus(),
                                history.getReason(), history.getActorType(), history.getActorLabel(),
                                history.getCreatedAt()))
                        .toList()
                : List.of();
        boolean editable = offer.getStatus() == JobOfferStatus.DRAFT
                || offer.getStatus() == JobOfferStatus.RETURNED;
        return new JobOfferResponse(
                offer.getId(), offer.getApplication().getId(), offer.getApplication().getCandidate().getId(),
                offer.getApplication().getCandidate().getFullName(),
                offer.getApplication().getCandidate().getEmail(),
                offer.getApplication().getJobOpening().getJobTitle(),
                offer.getApplication().getJobOpening().getDepartment().getId(),
                offer.getApplication().getJobOpening().getDepartment().getName(),
                offer.getStatus(), offer.getAnnualSalary(), offer.getCurrency(),
                offer.getProposedStartDate(), offer.getExpiresAt(), offer.getTitle(), offer.getMessage(),
                offer.getCreatedBy().getDisplayName(),
                offer.getApprovedBy() == null ? null : offer.getApprovedBy().getDisplayName(),
                offer.getSentAt(), offer.getRespondedAt(), offer.getResponseReason(), offer.getVersion(),
                offer.getCreatedAt(), offer.getUpdatedAt(),
                hr && editable, hr && editable,
                manager && offer.getStatus() == JobOfferStatus.PENDING_APPROVAL,
                hr && offer.getStatus() == JobOfferStatus.APPROVED,
                hr && !CLOSED_STATUSES.contains(offer.getStatus()), offerHistories);
    }

    private PublicJobOfferResponse publicResponse(JobOffer offer) {
        return new PublicJobOfferResponse(
                offer.getId(), "HireFlow", offer.getApplication().getCandidate().getFullName(),
                offer.getApplication().getJobOpening().getJobTitle(), offer.getStatus(),
                offer.getAnnualSalary(), offer.getCurrency(), offer.getProposedStartDate(),
                offer.getExpiresAt(), offer.getTitle(), offer.getMessage(),
                offer.getSentAt(), offer.getRespondedAt());
    }

    private void history(JobOffer offer, JobOfferStatus from, JobOfferStatus to, String reason,
            AppUser actor, OfferActorType actorType, String actorLabel) {
        histories.save(new JobOfferHistory(
                offer, from, to, reason, actor, actorType, actorLabel));
    }

    private AppUser actor(String emailAddress) {
        return users.findByEmailIgnoreCase(emailAddress)
                .orElseThrow(() -> new UnauthorizedException("登入狀態無效"));
    }

    private Application application(Long id) {
        return applications.findOneById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到應徵紀錄"));
    }

    private JobOffer offer(Long id) {
        return offers.findOneById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到 Offer"));
    }

    private void requireHr(AppUser actor) {
        if (!allDepartments(actor)) throw new ForbiddenException("只有 HR 可以維護 Offer");
    }

    private void requireManagerFor(AppUser actor, JobOffer offer) {
        if (has(actor, "ADMIN")) return;
        boolean sameDepartment = actor.getDepartment() != null
                && actor.getDepartment().getId().equals(
                        offer.getApplication().getJobOpening().getDepartment().getId());
        if (!has(actor, "HIRING_MANAGER") || !sameDepartment) {
            throw new ForbiddenException("只有該部門用人主管可以核准 Offer");
        }
    }

    private void ensureView(AppUser actor, JobOffer offer) {
        if (allDepartments(actor)) return;
        boolean sameDepartment = actor.getDepartment() != null
                && actor.getDepartment().getId().equals(
                        offer.getApplication().getJobOpening().getDepartment().getId());
        if (!sameDepartment) throw new ForbiddenException("你只能查看自己部門的 Offer");
    }

    private boolean allDepartments(AppUser actor) {
        return has(actor, "HR") || has(actor, "ADMIN");
    }

    private Long departmentId(AppUser actor) {
        if (actor.getDepartment() == null) throw new BusinessException("帳號尚未設定部門");
        return actor.getDepartment().getId();
    }

    private boolean has(AppUser actor, String role) {
        return actor.getRoles().stream().anyMatch(item -> item.getCode().equals(role));
    }
}
