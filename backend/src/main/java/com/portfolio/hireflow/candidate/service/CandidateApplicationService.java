package com.portfolio.hireflow.candidate.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.audit.entity.AuditAction;
import com.portfolio.hireflow.audit.entity.AuditTargetType;
import com.portfolio.hireflow.audit.service.AuditLogService;
import com.portfolio.hireflow.candidate.dto.ApplicationHistoryResponse;
import com.portfolio.hireflow.candidate.dto.ApplicationResponse;
import com.portfolio.hireflow.candidate.dto.CandidateRequest;
import com.portfolio.hireflow.candidate.dto.CandidateResponse;
import com.portfolio.hireflow.candidate.dto.CandidateResumeResponse;
import com.portfolio.hireflow.candidate.dto.CreateApplicationRequest;
import com.portfolio.hireflow.candidate.dto.TransitionApplicationRequest;
import com.portfolio.hireflow.candidate.entity.Application;
import com.portfolio.hireflow.candidate.entity.ApplicationStage;
import com.portfolio.hireflow.candidate.entity.ApplicationStageHistory;
import com.portfolio.hireflow.candidate.entity.Candidate;
import com.portfolio.hireflow.candidate.repository.ApplicationRepository;
import com.portfolio.hireflow.candidate.repository.ApplicationStageHistoryRepository;
import com.portfolio.hireflow.candidate.repository.CandidateRepository;
import com.portfolio.hireflow.candidate.repository.CandidateProfileRepository;
import com.portfolio.hireflow.candidate.repository.WorkExperienceRepository;
import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.common.exception.ConflictException;
import com.portfolio.hireflow.common.exception.ForbiddenException;
import com.portfolio.hireflow.common.exception.ResourceNotFoundException;
import com.portfolio.hireflow.common.exception.UnauthorizedException;
import com.portfolio.hireflow.common.response.PageResponse;
import com.portfolio.hireflow.decision.service.DecisionReadinessPolicy;
import com.portfolio.hireflow.decision.entity.HiringDecision;
import com.portfolio.hireflow.requisition.entity.JobOpening;
import com.portfolio.hireflow.requisition.entity.JobOpeningStatus;
import com.portfolio.hireflow.requisition.repository.JobOpeningRepository;

@Service
@Transactional(readOnly = true)
public class CandidateApplicationService {

    private static final Map<ApplicationStage, Set<ApplicationStage>> ALLOWED_TRANSITIONS = Map.of(
            ApplicationStage.NEW, Set.of(ApplicationStage.SCREENING, ApplicationStage.WITHDRAWN),
            ApplicationStage.SCREENING, Set.of(
                    ApplicationStage.INTERVIEW_SCHEDULED, ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN),
            ApplicationStage.INTERVIEW_SCHEDULED, Set.of(ApplicationStage.INTERVIEWED, ApplicationStage.WITHDRAWN),
            ApplicationStage.INTERVIEWED, Set.of(ApplicationStage.DECISION_PENDING, ApplicationStage.WITHDRAWN),
            ApplicationStage.DECISION_PENDING, Set.of(ApplicationStage.WITHDRAWN)
    );

    private final CandidateRepository candidates;
    private final ApplicationRepository applications;
    private final ApplicationStageHistoryRepository histories;
    private final JobOpeningRepository openings;
    private final AppUserRepository users;
    private final DecisionReadinessPolicy decisionReadinessPolicy;
    private final AuditLogService auditLogService;
    private final CandidateProfileRepository profiles;
    private final WorkExperienceRepository experiences;

    public CandidateApplicationService(
            CandidateRepository candidates,
            ApplicationRepository applications,
            ApplicationStageHistoryRepository histories,
            JobOpeningRepository openings,
            AppUserRepository users,
            DecisionReadinessPolicy decisionReadinessPolicy,
            AuditLogService auditLogService,
            CandidateProfileRepository profiles,
            WorkExperienceRepository experiences
    ) {
        this.candidates = candidates;
        this.applications = applications;
        this.histories = histories;
        this.openings = openings;
        this.users = users;
        this.decisionReadinessPolicy = decisionReadinessPolicy;
        this.auditLogService = auditLogService;
        this.profiles = profiles;
        this.experiences = experiences;
    }

    public PageResponse<CandidateResponse> findCandidates(String email, String keyword, Pageable pageable) {
        AppUser actor = loadActor(email);
        String normalizedKeyword = normalizeKeyword(keyword);
        Page<Candidate> page;
        if (canAccessAllDepartments(actor)) {
            page = normalizedKeyword == null
                    ? candidates.findAll(pageable)
                    : candidates.searchAll(normalizedKeyword, pageable);
        } else {
            Long departmentId = requireDepartmentId(actor);
            page = normalizedKeyword == null
                    ? candidates.searchByDepartment(departmentId, pageable)
                    : candidates.searchByDepartment(departmentId, normalizedKeyword, pageable);
        }
        return PageResponse.from(page.map(this::toCandidateResponse));
    }

    @Transactional
    public CandidateResponse createCandidate(String email, CandidateRequest request) {
        requireHr(loadActor(email));
        candidates.findByEmailIgnoreCase(request.email()).ifPresent(existing -> {
            throw new ConflictException("此 Email 已有應徵者資料，編號 #" + existing.getId());
        });

        Candidate candidate = new Candidate(request.fullName(), request.email());
        candidate.update(
                request.fullName(), request.email(), request.phone(), request.currentPosition(),
                request.yearsExperience(), request.skills(), request.notes());
        return toCandidateResponse(candidates.save(candidate));
    }

    @Transactional
    public CandidateResponse updateCandidate(Long id, String email, CandidateRequest request) {
        requireHr(loadActor(email));
        Candidate candidate = candidates.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到應徵者"));
        if (request.version() == null || request.version() != candidate.getVersion()) {
            throw new ConflictException("資料已被更新，請重新整理");
        }
        candidates.findByEmailIgnoreCase(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new ConflictException("此 Email 已有應徵者資料"); });

        candidate.update(
                request.fullName(), request.email(), request.phone(), request.currentPosition(),
                request.yearsExperience(), request.skills(), request.notes());
        return toCandidateResponse(candidate);
    }

    public PageResponse<ApplicationResponse> findApplications(
            String email, ApplicationStage stage, Long openingId, String keyword, Pageable pageable
    ) {
        return findApplications(email, stage, openingId, keyword, false, pageable);
    }

    public PageResponse<ApplicationResponse> findApplications(
            String email, ApplicationStage stage, Long openingId, String keyword,
            boolean decisionOnly, Pageable pageable
    ) {
        AppUser actor = loadActor(email);
        Specification<Application> specification = (root, query, builder) -> builder.conjunction();

        if (!canAccessAllDepartments(actor)) {
            Long departmentId = requireDepartmentId(actor);
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("jobOpening").get("department").get("id"), departmentId));
        }
        if (decisionOnly) {
            specification = specification.and((root, query, builder) -> {
                var decision = query.subquery(Long.class);
                var decisionRoot = decision.from(HiringDecision.class);
                decision.select(decisionRoot.get("id"));
                decision.where(builder.equal(decisionRoot.get("application").get("id"), root.get("id")));
                return builder.or(
                        root.get("stage").in(ApplicationStage.INTERVIEWED,
                                ApplicationStage.DECISION_PENDING, ApplicationStage.HIRED,
                                ApplicationStage.ON_HOLD),
                        builder.and(builder.equal(root.get("stage"), ApplicationStage.REJECTED),
                                builder.exists(decision)));
            });
        }
        if (stage != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("stage"), stage));
        }
        if (openingId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("jobOpening").get("id"), openingId));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("candidate").get("fullName")), pattern),
                    builder.like(builder.lower(root.get("candidate").get("email")), pattern),
                    builder.like(builder.lower(root.get("jobOpening").get("jobTitle")), pattern)
            ));
        }

        return PageResponse.from(applications.findAll(specification, pageable)
                .map(application -> toApplicationResponse(application, false)));
    }

    public ApplicationResponse findApplication(Long id, String email) {
        AppUser actor = loadActor(email);
        Application application = loadApplication(id);
        ensureCanAccess(actor, application);
        return toApplicationResponse(application, true);
    }

    public CandidateResumeResponse findCandidateResume(Long id, String email) {
        AppUser actor = loadActor(email);
        Candidate candidate = candidates.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到應徵者"));
        if (!canAccessAllDepartments(actor)
                && !applications.existsByCandidateIdAndJobOpeningDepartmentId(id, requireDepartmentId(actor))) {
            throw new ForbiddenException("你只能查看自己部門的應徵履歷");
        }
        var profile = profiles.findByCandidateId(id).orElse(null);
        var work = experiences.findByCandidateIdOrderBySortOrder(id).stream()
                .map(item -> new CandidateResumeResponse.WorkExperienceResponse(
                        item.getCompanyName(), item.getJobTitle(), item.getStartDate(), item.getEndDate(),
                        item.isCurrentJob(), item.getDescription()))
                .toList();
        return new CandidateResumeResponse(candidate.getId(), candidate.getFullName(), candidate.getEmail(),
                candidate.getPhone(), candidate.getCurrentPosition(), candidate.getYearsExperience(),
                candidate.getSkills(), profile == null ? null : profile.getAddress(),
                profile == null ? null : profile.getAutobiography(),
                profile == null ? null : profile.getPortfolioUrl(), work);
    }

    @Transactional
    public ApplicationResponse createApplication(String email, CreateApplicationRequest request) {
        AppUser actor = loadActor(email);
        requireHr(actor);
        Candidate candidate = candidates.findById(request.candidateId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到應徵者"));
        JobOpening opening = openings.findOneById(request.jobOpeningId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到職缺"));

        if (opening.getStatus() != JobOpeningStatus.OPEN) {
            throw new BusinessException("已關閉的職缺不能新增應徵紀錄");
        }
        if (applications.existsByCandidateIdAndJobOpeningId(candidate.getId(), opening.getId())) {
            throw new ConflictException("此應徵者已經應徵過該職缺");
        }

        Application application = applications.save(new Application(candidate, opening, actor));
        histories.save(new ApplicationStageHistory(application, null, ApplicationStage.NEW, null, actor));
        auditLogService.record(
                actor, AuditAction.APPLICATION_CREATED, AuditTargetType.APPLICATION, application.getId(),
                "Candidate #" + candidate.getId() + " / Job opening #" + opening.getId());
        return toApplicationResponse(application, true);
    }

    @Transactional
    public ApplicationResponse transition(Long id, String email, TransitionApplicationRequest request) {
        AppUser actor = loadActor(email);
        requireHr(actor);
        Application application = loadApplication(id);
        ensureCanAccess(actor, application);

        Set<ApplicationStage> allowed = ALLOWED_TRANSITIONS.getOrDefault(application.getStage(), Set.of());
        if (!allowed.contains(request.nextStage())) {
            throw new BusinessException(
                    "不允許從 " + application.getStage() + " 轉換到 " + request.nextStage());
        }
        if (requiresReason(request.nextStage()) && (request.reason() == null || request.reason().isBlank())) {
            throw new BusinessException("拒絕或退出時必須填寫原因");
        }
        if (request.nextStage() == ApplicationStage.DECISION_PENDING) {
            decisionReadinessPolicy.verify(application.getId());
        }

        ApplicationStage previousStage = application.getStage();
        application.transitionTo(request.nextStage());
        histories.save(new ApplicationStageHistory(
                application, previousStage, request.nextStage(), request.reason(), actor));
        auditLogService.record(
                actor, AuditAction.APPLICATION_STAGE_CHANGED, AuditTargetType.APPLICATION, application.getId(),
                previousStage + " -> " + request.nextStage());
        return toApplicationResponse(application, true);
    }

    private CandidateResponse toCandidateResponse(Candidate candidate) {
        return new CandidateResponse(
                candidate.getId(), candidate.getFullName(), candidate.getEmail(), candidate.getPhone(),
                candidate.getCurrentPosition(), candidate.getYearsExperience(), candidate.getSkills(),
                candidate.getNotes(), candidate.getVersion(), candidate.getCreatedAt(), candidate.getUpdatedAt());
    }

    private ApplicationResponse toApplicationResponse(Application application, boolean includeHistory) {
        List<ApplicationHistoryResponse> history = includeHistory
                ? histories.findByApplicationIdOrderByCreatedAtAscIdAsc(application.getId()).stream()
                        .map(item -> new ApplicationHistoryResponse(
                                item.getId(), item.getFromStage(), item.getToStage(), item.getReason(),
                                item.getActor().getId(), item.getActor().getDisplayName(), item.getCreatedAt()))
                        .toList()
                : List.of();
        JobOpening opening = application.getJobOpening();
        Candidate candidate = application.getCandidate();
        return new ApplicationResponse(
                application.getId(), candidate.getId(), candidate.getFullName(), candidate.getEmail(),
                opening.getId(), opening.getJobTitle(), opening.getDepartment().getId(),
                opening.getDepartment().getName(), application.getStage(), application.getVersion(),
                application.getCreatedAt(), application.getUpdatedAt(), history);
    }

    private AppUser loadActor(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("登入狀態無效"));
    }

    private Application loadApplication(Long id) {
        return applications.findOneById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到應徵紀錄"));
    }

    private void ensureCanAccess(AppUser actor, Application application) {
        if (!canAccessAllDepartments(actor)
                && !requireDepartmentId(actor).equals(application.getJobOpening().getDepartment().getId())) {
            throw new ForbiddenException("你只能存取自己部門的應徵資料");
        }
    }

    private void requireHr(AppUser actor) {
        if (!canAccessAllDepartments(actor)) {
            throw new ForbiddenException("只有 HR 可以維護應徵者與建立應徵紀錄");
        }
    }

    private boolean canAccessAllDepartments(AppUser actor) {
        return hasRole(actor, "HR") || hasRole(actor, "ADMIN");
    }

    private boolean hasRole(AppUser actor, String code) {
        return actor.getRoles().stream().anyMatch(role -> role.getCode().equals(code));
    }

    private Long requireDepartmentId(AppUser actor) {
        if (actor.getDepartment() == null) {
            throw new BusinessException("帳號尚未設定部門");
        }
        return actor.getDepartment().getId();
    }

    private static boolean requiresReason(ApplicationStage stage) {
        return stage == ApplicationStage.REJECTED || stage == ApplicationStage.WITHDRAWN;
    }

    private static String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
