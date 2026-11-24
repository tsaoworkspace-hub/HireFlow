package com.portfolio.hireflow.requisition.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.entity.Department;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.auth.repository.DepartmentRepository;
import com.portfolio.hireflow.audit.entity.AuditAction;
import com.portfolio.hireflow.audit.entity.AuditTargetType;
import com.portfolio.hireflow.audit.service.AuditLogService;
import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.common.exception.ConflictException;
import com.portfolio.hireflow.common.exception.ForbiddenException;
import com.portfolio.hireflow.common.exception.ResourceNotFoundException;
import com.portfolio.hireflow.common.exception.UnauthorizedException;
import com.portfolio.hireflow.common.response.PageResponse;
import com.portfolio.hireflow.requisition.dto.CreateRequisitionRequest;
import com.portfolio.hireflow.requisition.dto.DepartmentOptionResponse;
import com.portfolio.hireflow.requisition.dto.JobOpeningResponse;
import com.portfolio.hireflow.requisition.dto.RequisitionActionResponse;
import com.portfolio.hireflow.requisition.dto.RequisitionOptionsResponse;
import com.portfolio.hireflow.requisition.dto.RequisitionResponse;
import com.portfolio.hireflow.requisition.dto.ReviewRequisitionRequest;
import com.portfolio.hireflow.requisition.dto.UpdateRequisitionRequest;
import com.portfolio.hireflow.requisition.entity.JobOpening;
import com.portfolio.hireflow.requisition.entity.JobRequisition;
import com.portfolio.hireflow.requisition.entity.RequisitionAction;
import com.portfolio.hireflow.requisition.entity.RequisitionActionType;
import com.portfolio.hireflow.requisition.entity.RequisitionStatus;
import com.portfolio.hireflow.requisition.repository.JobOpeningRepository;
import com.portfolio.hireflow.requisition.repository.JobRequisitionRepository;
import com.portfolio.hireflow.requisition.repository.RequisitionActionRepository;

@Service
@Transactional(readOnly = true)
public class RequisitionService {

    private final JobRequisitionRepository requisitionRepository;
    private final RequisitionActionRepository actionRepository;
    private final JobOpeningRepository jobOpeningRepository;
    private final AppUserRepository appUserRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogService auditLogService;

    public RequisitionService(
            JobRequisitionRepository requisitionRepository,
            RequisitionActionRepository actionRepository,
            JobOpeningRepository jobOpeningRepository,
            AppUserRepository appUserRepository,
            DepartmentRepository departmentRepository,
            AuditLogService auditLogService
    ) {
        this.requisitionRepository = requisitionRepository;
        this.actionRepository = actionRepository;
        this.jobOpeningRepository = jobOpeningRepository;
        this.appUserRepository = appUserRepository;
        this.departmentRepository = departmentRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<RequisitionResponse> findAll(
            String actorEmail,
            RequisitionStatus status,
            Long departmentId,
            Long createdById,
            String keyword,
            Pageable pageable
    ) {
        AppUser actor = loadActor(actorEmail);
        Specification<JobRequisition> specification = (root, query, builder) -> builder.conjunction();

        if (!canAccessAllDepartments(actor)) {
            Long actorDepartmentId = requireDepartment(actor).getId();
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("department").get("id"), actorDepartmentId));
        } else if (departmentId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("department").get("id"), departmentId));
        }

        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        if (createdById != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("createdBy").get("id"), createdById));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("jobTitle")), pattern),
                    builder.like(builder.lower(root.get("reason")), pattern),
                    builder.like(builder.lower(root.get("requiredSkills")), pattern)
            ));
        }

        Page<RequisitionResponse> page = requisitionRepository.findAll(specification, pageable)
                .map(requisition -> toResponse(requisition, actor, false));
        return PageResponse.from(page);
    }

    public RequisitionOptionsResponse getOptions(String actorEmail) {
        AppUser actor = loadActor(actorEmail);
        List<Department> departments = canAccessAllDepartments(actor)
                ? departmentRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                : List.of(requireDepartment(actor));
        List<DepartmentOptionResponse> departmentOptions = departments.stream()
                .map(department -> new DepartmentOptionResponse(department.getId(), department.getName()))
                .toList();
        List<String> statuses = Arrays.stream(RequisitionStatus.values()).map(Enum::name).toList();
        return new RequisitionOptionsResponse(departmentOptions, statuses);
    }

    public RequisitionResponse findById(Long id, String actorEmail) {
        AppUser actor = loadActor(actorEmail);
        JobRequisition requisition = loadRequisition(id);
        ensureCanView(actor, requisition);
        return toResponse(requisition, actor, true);
    }

    @Transactional
    public RequisitionResponse create(CreateRequisitionRequest request, String actorEmail) {
        AppUser actor = loadActor(actorEmail);
        Department department = loadDepartment(request.departmentId());
        ensureCanUseDepartment(actor, department);
        validateSalaryRange(request.salaryMin(), request.salaryMax());

        JobRequisition requisition = new JobRequisition(department, actor);
        requisition.update(
                department,
                request.jobTitle(),
                request.headcount(),
                request.reason(),
                request.requiredSkills(),
                request.salaryMin(),
                request.salaryMax(),
                request.targetStartDate()
        );
        requisitionRepository.save(requisition);
        recordAction(requisition, RequisitionActionType.CREATED, null, RequisitionStatus.DRAFT, null, actor);
        recordAudit(actor, AuditAction.REQUISITION_CREATED, requisition);
        return toResponse(requisition, actor, true);
    }

    @Transactional
    public RequisitionResponse update(Long id, UpdateRequisitionRequest request, String actorEmail) {
        AppUser actor = loadActor(actorEmail);
        JobRequisition requisition = loadRequisition(id);
        ensureCanEdit(actor, requisition);
        if (request.version() != requisition.getVersion()) {
            throw new ConflictException("資料已被更新，請重新整理後再編輯");
        }

        Department department = loadDepartment(request.departmentId());
        ensureCanUseDepartment(actor, department);
        validateSalaryRange(request.salaryMin(), request.salaryMax());
        requisition.update(
                department,
                request.jobTitle(),
                request.headcount(),
                request.reason(),
                request.requiredSkills(),
                request.salaryMin(),
                request.salaryMax(),
                request.targetStartDate()
        );
        recordAction(
                requisition,
                RequisitionActionType.UPDATED,
                requisition.getStatus(),
                requisition.getStatus(),
                null,
                actor
        );
        recordAudit(actor, AuditAction.REQUISITION_UPDATED, requisition);
        return toResponse(requisition, actor, true);
    }

    @Transactional
    public RequisitionResponse submit(Long id, String actorEmail) {
        AppUser actor = loadActor(actorEmail);
        JobRequisition requisition = loadRequisition(id);
        ensureCanSubmit(actor, requisition);
        validateComplete(requisition);

        RequisitionStatus previousStatus = requisition.getStatus();
        requisition.submit();
        recordAction(
                requisition,
                RequisitionActionType.SUBMITTED,
                previousStatus,
                RequisitionStatus.SUBMITTED,
                null,
                actor
        );
        recordAudit(actor, AuditAction.REQUISITION_SUBMITTED, requisition);
        return toResponse(requisition, actor, true);
    }

    @Transactional
    public RequisitionResponse review(Long id, ReviewRequisitionRequest request, String actorEmail) {
        AppUser actor = loadActor(actorEmail);
        ensureReviewer(actor);
        JobRequisition requisition = loadRequisition(id);
        if (requisition.getStatus() != RequisitionStatus.SUBMITTED) {
            throw new BusinessException("只有待審核的需求可以進行審核");
        }

        RequisitionActionType actionType;
        RequisitionStatus nextStatus;
        switch (request.decision()) {
            case APPROVE -> {
                if (jobOpeningRepository.existsByRequisitionId(id)) {
                    throw new ConflictException("此需求已經建立職缺");
                }
                validateComplete(requisition);
                requisition.approve();
                jobOpeningRepository.save(new JobOpening(requisition, actor));
                actionType = RequisitionActionType.APPROVED;
                nextStatus = RequisitionStatus.APPROVED;
            }
            case RETURN -> {
                requireReason(request.reason(), "退回理由為必填");
                requisition.returnForRevision();
                actionType = RequisitionActionType.RETURNED;
                nextStatus = RequisitionStatus.RETURNED;
            }
            case REJECT -> {
                requireReason(request.reason(), "拒絕理由為必填");
                requisition.reject();
                actionType = RequisitionActionType.REJECTED;
                nextStatus = RequisitionStatus.REJECTED;
            }
            default -> throw new BusinessException("不支援的審核結果");
        }

        recordAction(
                requisition,
                actionType,
                RequisitionStatus.SUBMITTED,
                nextStatus,
                request.reason(),
                actor
        );
        recordAudit(actor, switch (actionType) {
            case APPROVED -> AuditAction.REQUISITION_APPROVED;
            case RETURNED -> AuditAction.REQUISITION_RETURNED;
            case REJECTED -> AuditAction.REQUISITION_REJECTED;
            default -> throw new IllegalStateException("Unexpected review action: " + actionType);
        }, requisition);
        return toResponse(requisition, actor, true);
    }

    private void validateComplete(JobRequisition requisition) {
        if (isBlank(requisition.getJobTitle())
                || requisition.getHeadcount() == null
                || requisition.getHeadcount() < 1
                || isBlank(requisition.getReason())
                || isBlank(requisition.getRequiredSkills())
                || requisition.getSalaryMin() == null
                || requisition.getSalaryMax() == null
                || requisition.getTargetStartDate() == null) {
            throw new BusinessException("送出前請完整填寫職稱、人數、原因、技能、薪資區間與希望到職日");
        }
        validateSalaryRange(requisition.getSalaryMin(), requisition.getSalaryMax());
    }

    private void validateSalaryRange(BigDecimal salaryMin, BigDecimal salaryMax) {
        if (salaryMin != null && salaryMax != null && salaryMax.compareTo(salaryMin) < 0) {
            throw new BusinessException("最高薪資不可低於最低薪資");
        }
    }

    private void ensureCanView(AppUser actor, JobRequisition requisition) {
        if (canAccessAllDepartments(actor)) {
            return;
        }
        Department department = requireDepartment(actor);
        if (!hasRole(actor, "HIRING_MANAGER")
                || !department.getId().equals(requisition.getDepartment().getId())) {
            throw new ForbiddenException("你只能查看自己部門的人力需求");
        }
    }

    private void ensureCanEdit(AppUser actor, JobRequisition requisition) {
        ensureCanView(actor, requisition);
        if (requisition.getStatus() != RequisitionStatus.DRAFT
                && requisition.getStatus() != RequisitionStatus.RETURNED) {
            throw new BusinessException("只有草稿或退回的需求可以修改");
        }
        if (!isCreator(actor, requisition) && !canAccessAllDepartments(actor)) {
            throw new ForbiddenException("只有建立者或 HR 可以修改此需求");
        }
    }

    private void ensureCanSubmit(AppUser actor, JobRequisition requisition) {
        ensureCanView(actor, requisition);
        if (requisition.getStatus() != RequisitionStatus.DRAFT
                && requisition.getStatus() != RequisitionStatus.RETURNED) {
            throw new BusinessException("只有草稿或退回的需求可以送出");
        }
        if (!isCreator(actor, requisition) && !hasRole(actor, "ADMIN")) {
            throw new ForbiddenException("只有需求建立者可以送出審核");
        }
    }

    private void ensureCanUseDepartment(AppUser actor, Department department) {
        if (canAccessAllDepartments(actor)) {
            return;
        }
        Department actorDepartment = requireDepartment(actor);
        if (!hasRole(actor, "HIRING_MANAGER") || !actorDepartment.getId().equals(department.getId())) {
            throw new ForbiddenException("你只能建立或修改自己部門的人力需求");
        }
    }

    private void ensureReviewer(AppUser actor) {
        if (!canAccessAllDepartments(actor)) {
            throw new ForbiddenException("只有 HR 可以審核人力需求");
        }
    }

    private AppUser loadActor(String email) {
        return appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("登入狀態無效"));
    }

    private JobRequisition loadRequisition(Long id) {
        return requisitionRepository.findOneById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到指定的人力需求"));
    }

    private Department loadDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到指定的部門"));
    }

    private Department requireDepartment(AppUser actor) {
        if (actor.getDepartment() == null) {
            throw new BusinessException("目前帳號尚未設定部門");
        }
        return actor.getDepartment();
    }

    private void recordAction(
            JobRequisition requisition,
            RequisitionActionType actionType,
            RequisitionStatus fromStatus,
            RequisitionStatus toStatus,
            String reason,
            AppUser actor
    ) {
        actionRepository.save(new RequisitionAction(
                requisition,
                actionType,
                fromStatus,
                toStatus,
                reason,
                actor
        ));
    }

    private void recordAudit(AppUser actor, AuditAction action, JobRequisition requisition) {
        auditLogService.record(
                actor, action, AuditTargetType.REQUISITION, requisition.getId(),
                "Status: " + requisition.getStatus());
    }

    private RequisitionResponse toResponse(JobRequisition requisition, AppUser actor, boolean includeDetails) {
        boolean editableStatus = requisition.getStatus() == RequisitionStatus.DRAFT
                || requisition.getStatus() == RequisitionStatus.RETURNED;
        boolean canEdit = editableStatus && (isCreator(actor, requisition) || canAccessAllDepartments(actor));
        boolean canSubmit = editableStatus && (isCreator(actor, requisition) || hasRole(actor, "ADMIN"));
        boolean canReview = requisition.getStatus() == RequisitionStatus.SUBMITTED && canAccessAllDepartments(actor);

        JobOpeningResponse opening = null;
        List<RequisitionActionResponse> actions = List.of();
        if (includeDetails) {
            opening = jobOpeningRepository.findByRequisitionId(requisition.getId())
                    .map(value -> new JobOpeningResponse(value.getId(), value.getStatus(), value.getCreatedAt()))
                    .orElse(null);
            actions = actionRepository.findByRequisitionIdOrderByCreatedAtAscIdAsc(requisition.getId()).stream()
                    .map(action -> new RequisitionActionResponse(
                            action.getId(),
                            action.getActionType(),
                            action.getFromStatus(),
                            action.getToStatus(),
                            action.getReason(),
                            action.getActor().getId(),
                            action.getActor().getDisplayName(),
                            action.getCreatedAt()
                    ))
                    .toList();
        }

        return new RequisitionResponse(
                requisition.getId(),
                requisition.getDepartment().getId(),
                requisition.getDepartment().getName(),
                requisition.getJobTitle(),
                requisition.getHeadcount(),
                requisition.getReason(),
                requisition.getRequiredSkills(),
                requisition.getSalaryMin(),
                requisition.getSalaryMax(),
                requisition.getTargetStartDate(),
                requisition.getStatus(),
                requisition.getCreatedBy().getId(),
                requisition.getCreatedBy().getDisplayName(),
                requisition.getCreatedBy().getEmail(),
                requisition.getVersion(),
                requisition.getCreatedAt(),
                requisition.getUpdatedAt(),
                requisition.getSubmittedAt(),
                requisition.getReviewedAt(),
                canEdit,
                canSubmit,
                canReview,
                opening,
                actions
        );
    }

    private boolean canAccessAllDepartments(AppUser user) {
        return hasRole(user, "HR") || hasRole(user, "ADMIN");
    }

    private boolean isCreator(AppUser actor, JobRequisition requisition) {
        return actor.getId().equals(requisition.getCreatedBy().getId());
    }

    private boolean hasRole(AppUser user, String code) {
        return user.getRoles().stream().anyMatch(role -> role.getCode().equals(code));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void requireReason(String reason, String message) {
        if (isBlank(reason)) {
            throw new BusinessException(message);
        }
    }
}
