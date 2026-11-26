package com.portfolio.hireflow.interview.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.audit.entity.AuditAction;
import com.portfolio.hireflow.audit.entity.AuditTargetType;
import com.portfolio.hireflow.audit.service.AuditLogService;
import com.portfolio.hireflow.candidate.entity.Application;
import com.portfolio.hireflow.candidate.entity.ApplicationStage;
import com.portfolio.hireflow.candidate.entity.ApplicationStageHistory;
import com.portfolio.hireflow.candidate.repository.ApplicationRepository;
import com.portfolio.hireflow.candidate.repository.ApplicationStageHistoryRepository;
import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.common.exception.ConflictException;
import com.portfolio.hireflow.common.exception.ForbiddenException;
import com.portfolio.hireflow.common.exception.ResourceNotFoundException;
import com.portfolio.hireflow.common.exception.UnauthorizedException;
import com.portfolio.hireflow.common.response.PageResponse;
import com.portfolio.hireflow.interview.dto.*;
import com.portfolio.hireflow.interview.entity.*;
import com.portfolio.hireflow.interview.repository.InterviewRepository;
import com.portfolio.hireflow.interview.repository.InterviewScorecardRepository;
import com.portfolio.hireflow.requisition.entity.JobOpeningStatus;
import com.portfolio.hireflow.integration.calendar.service.CalendarSyncQueueService;

@Service
@Transactional(readOnly = true)
public class InterviewService {

    private static final Set<ApplicationStage> SCHEDULABLE_STAGES = Set.of(
            ApplicationStage.SCREENING,
            ApplicationStage.INTERVIEW_SCHEDULED,
            ApplicationStage.INTERVIEWED
    );

    private final InterviewRepository interviews;
    private final InterviewScorecardRepository scorecards;
    private final ApplicationRepository applications;
    private final ApplicationStageHistoryRepository applicationHistories;
    private final AppUserRepository users;
    private final AuditLogService auditLogService;
    private final CalendarSyncQueueService calendarSyncQueueService;

    public InterviewService(
            InterviewRepository interviews,
            InterviewScorecardRepository scorecards,
            ApplicationRepository applications,
            ApplicationStageHistoryRepository applicationHistories,
            AppUserRepository users,
            AuditLogService auditLogService,
            CalendarSyncQueueService calendarSyncQueueService
    ) {
        this.interviews = interviews;
        this.scorecards = scorecards;
        this.applications = applications;
        this.applicationHistories = applicationHistories;
        this.users = users;
        this.auditLogService = auditLogService;
        this.calendarSyncQueueService = calendarSyncQueueService;
    }

    public PageResponse<InterviewResponse> findAll(
            String email,
            InterviewStatus status,
            Long applicationId,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        AppUser actor = loadActor(email);
        Specification<Interview> specification = (root, query, builder) -> builder.conjunction();

        if (isInterviewerOnly(actor)) {
            specification = specification.and((root, query, builder) -> {
                query.distinct(true);
                return builder.equal(root.join("interviewers").get("id"), actor.getId());
            });
        } else if (!canAccessAll(actor)) {
            Long departmentId = requireDepartmentId(actor);
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("application").get("jobOpening").get("department").get("id"), departmentId));
        }
        if (status != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (applicationId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("application").get("id"), applicationId));
        }
        if (from != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("startsAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, builder) -> builder.lessThan(root.get("startsAt"), to));
        }

        return PageResponse.from(interviews.findAll(specification, pageable)
                .map(interview -> toResponse(interview, actor)));
    }

    public InterviewResponse findById(Long id, String email) {
        AppUser actor = loadActor(email);
        Interview interview = loadInterview(id);
        ensureCanView(actor, interview);
        return toResponse(interview, actor);
    }

    public InterviewOptionsResponse getOptions(String email) {
        AppUser actor = loadActor(email);
        requireHr(actor);
        Specification<Application> specification = (root, query, builder) -> builder.and(
                root.get("stage").in(SCHEDULABLE_STAGES),
                builder.equal(root.get("jobOpening").get("status"), JobOpeningStatus.OPEN)
        );
        Page<Application> applicationPage = applications.findAll(
                specification, PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "updatedAt")));
        List<InterviewApplicationOptionResponse> applicationOptions = applicationPage.stream()
                .map(application -> new InterviewApplicationOptionResponse(
                        application.getId(), application.getCandidate().getFullName(),
                        application.getJobOpening().getJobTitle(), application.getJobOpening().getDepartment().getName(),
                        application.getStage()))
                .toList();
        List<InterviewerResponse> interviewerOptions = users.findAssignableInterviewers().stream()
                .map(this::toInterviewerResponse)
                .toList();
        return new InterviewOptionsResponse(applicationOptions, interviewerOptions);
    }

    @Transactional
    public InterviewResponse schedule(String email, ScheduleInterviewRequest request) {
        AppUser actor = loadActor(email);
        requireHr(actor);
        validateTimes(request.startsAt(), request.endsAt());

        Application application = applications.findOneById(request.applicationId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到應徵紀錄"));
        if (application.getJobOpening().getStatus() != JobOpeningStatus.OPEN) {
            throw new BusinessException("已關閉的職缺不能安排新面試");
        }
        if (!SCHEDULABLE_STAGES.contains(application.getStage())) {
            throw new BusinessException("目前應徵階段不能安排面試");
        }

        Set<AppUser> assignedInterviewers = new HashSet<>(users.findAllById(request.interviewerIds()));
        if (assignedInterviewers.size() != request.interviewerIds().size()) {
            throw new ResourceNotFoundException("部分面試官不存在");
        }
        for (AppUser interviewer : assignedInterviewers) {
            if (!canBeAssigned(interviewer)) {
                throw new BusinessException(interviewer.getDisplayName() + " 不是可指派的面試官");
            }
            if (interviews.countConflicts(
                    interviewer.getId(), request.startsAt(), request.endsAt(), InterviewStatus.CANCELLED) > 0) {
                throw new ConflictException(interviewer.getDisplayName() + " 在此時段已有其他面試");
            }
        }

        Interview interview = interviews.save(new Interview(
                application, request.roundNumber(), request.startsAt(), request.endsAt(), request.format(),
                request.location(), assignedInterviewers, actor));
        if (application.getStage() == ApplicationStage.SCREENING) {
            transitionApplication(application, ApplicationStage.INTERVIEW_SCHEDULED, "已安排面試", actor);
        }
        auditLogService.record(
                actor, AuditAction.INTERVIEW_SCHEDULED, AuditTargetType.INTERVIEW, interview.getId(),
                "Application #" + application.getId() + ", round " + interview.getRoundNumber());
        calendarSyncQueueService.queueUpserts(interview);
        return toResponse(interview, actor);
    }

    @Transactional
    public InterviewResponse reschedule(Long id, String email, RescheduleInterviewRequest request) {
        AppUser actor = loadActor(email);
        requireHr(actor);
        Interview interview = loadInterview(id);
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new BusinessException("只有已排程面試可以改期");
        }
        if (request.version() == null || interview.getVersion() != request.version()) {
            throw new ConflictException("面試資料已更新，請重新載入後再改期");
        }
        if (!SCHEDULABLE_STAGES.contains(interview.getApplication().getStage())) {
            throw new BusinessException("目前應徵階段不能改期");
        }
        if (scorecards.countByInterviewIdAndStatus(id, ScorecardStatus.SUBMITTED) > 0) {
            throw new BusinessException("已有送出評分，不能直接改期");
        }
        validateTimes(request.startsAt(), request.endsAt());
        for (AppUser interviewer : interview.getInterviewers()) {
            if (interviews.countConflictsExcluding(interviewer.getId(), id,
                    request.startsAt(), request.endsAt(), InterviewStatus.CANCELLED) > 0) {
                throw new ConflictException(interviewer.getDisplayName() + " 在此時段已有其他面試");
            }
        }
        interview.reschedule(request.startsAt(), request.endsAt(), request.format(), request.location());
        // Flush advances @Version before producing a distinct calendar work key.
        interviews.flush();
        calendarSyncQueueService.queueUpserts(interview);
        auditLogService.record(actor, AuditAction.INTERVIEW_RESCHEDULED,
                AuditTargetType.INTERVIEW, id, request.reason());
        return toResponse(interview, actor);
    }

    @Transactional
    public InterviewResponse cancel(Long id, String email, CancelInterviewRequest request) {
        AppUser actor = loadActor(email);
        requireHr(actor);
        Interview interview = loadInterview(id);
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new BusinessException("只有已排程面試可以取消");
        }
        interview.cancel(request.reason());
        auditLogService.record(
                actor, AuditAction.INTERVIEW_CANCELLED, AuditTargetType.INTERVIEW, interview.getId(), null);
        calendarSyncQueueService.queueDeletes(interview);
        return toResponse(interview, actor);
    }

    @Transactional
    public InterviewResponse complete(Long id, String email) {
        AppUser actor = loadActor(email);
        requireHr(actor);
        Interview interview = loadInterview(id);
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new BusinessException("只有已排程面試可以完成");
        }
        if (scorecards.countByInterviewIdAndStatus(id, ScorecardStatus.SUBMITTED) < 1) {
            throw new BusinessException("至少需要一份已送出的評分才能完成面試");
        }
        interview.complete();
        interviews.flush();

        Application application = interview.getApplication();
        if (interviews.countByApplicationIdAndStatus(application.getId(), InterviewStatus.SCHEDULED) == 0
                && application.getStage() == ApplicationStage.INTERVIEW_SCHEDULED) {
            transitionApplication(application, ApplicationStage.INTERVIEWED, "面試與評分已完成", actor);
        }
        auditLogService.record(
                actor, AuditAction.INTERVIEW_COMPLETED, AuditTargetType.INTERVIEW, interview.getId(),
                "Application #" + application.getId());
        return toResponse(interview, actor);
    }

    @Transactional
    public InterviewScorecardResponse saveScorecard(Long interviewId, String email, ScorecardRequest request) {
        AppUser actor = loadActor(email);
        Interview interview = loadInterview(interviewId);
        ensureAssigned(actor, interview);
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new BusinessException("已取消的面試不能填寫評分");
        }

        InterviewScorecard scorecard = scorecards.findByInterviewIdAndInterviewerId(interviewId, actor.getId())
                .orElseGet(() -> new InterviewScorecard(interview, actor));
        if (scorecard.getStatus() == ScorecardStatus.SUBMITTED) {
            throw new ConflictException("評分已送出並鎖定，請聯絡 HR 解鎖");
        }
        scorecard.update(
                request.technicalScore(), request.problemAnalysisScore(), request.communicationScore(),
                request.learningAbilityScore(), request.overallComment(), request.recommendation());
        if (request.submit()) {
            scorecard.submit();
        }
        InterviewScorecard saved = scorecards.save(scorecard);
        if (request.submit()) {
            auditLogService.record(
                    actor, AuditAction.SCORECARD_SUBMITTED, AuditTargetType.SCORECARD, saved.getId(),
                    "Interview #" + interviewId);
        }
        return toScorecardResponse(saved);
    }

    @Transactional
    public InterviewScorecardResponse unlockScorecard(
            Long interviewId, Long scorecardId, String email, UnlockScorecardRequest request
    ) {
        AppUser actor = loadActor(email);
        requireHr(actor);
        InterviewScorecard scorecard = scorecards.findById(scorecardId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到評分表"));
        if (!scorecard.getInterview().getId().equals(interviewId)) {
            throw new ResourceNotFoundException("找不到評分表");
        }
        if (scorecard.getStatus() != ScorecardStatus.SUBMITTED) {
            throw new BusinessException("只有已送出的評分可以解鎖");
        }
        scorecard.unlock(actor, request.reason());
        auditLogService.record(
                actor, AuditAction.SCORECARD_UNLOCKED, AuditTargetType.SCORECARD, scorecard.getId(),
                "Interview #" + interviewId);
        return toScorecardResponse(scorecard);
    }

    private InterviewResponse toResponse(Interview interview, AppUser actor) {
        List<InterviewScorecard> visibleScorecards = scorecards.findByInterviewIdOrderByCreatedAtAsc(interview.getId())
                .stream()
                .filter(scorecard -> !isInterviewerOnly(actor)
                        || scorecard.getInterviewer().getId().equals(actor.getId()))
                .toList();
        boolean assigned = interview.getInterviewers().stream().anyMatch(user -> user.getId().equals(actor.getId()));
        boolean alreadySubmitted = visibleScorecards.stream().anyMatch(scorecard ->
                scorecard.getInterviewer().getId().equals(actor.getId())
                        && scorecard.getStatus() == ScorecardStatus.SUBMITTED);
        Application application = interview.getApplication();
        return new InterviewResponse(
                interview.getId(), application.getId(), application.getCandidate().getId(),
                application.getCandidate().getFullName(), application.getCandidate().getEmail(),
                application.getJobOpening().getId(), application.getJobOpening().getJobTitle(),
                application.getJobOpening().getDepartment().getId(),
                application.getJobOpening().getDepartment().getName(), interview.getRoundNumber(),
                interview.getStartsAt(), interview.getEndsAt(), interview.getFormat(), interview.getLocation(),
                interview.getStatus(), interview.getCancellationReason(),
                interview.getInterviewers().stream().map(this::toInterviewerResponse).toList(),
                visibleScorecards.stream().map(this::toScorecardResponse).toList(),
                assigned && interview.getStatus() != InterviewStatus.CANCELLED && !alreadySubmitted,
                canAccessAll(actor), interview.getVersion(), interview.getCreatedAt(), interview.getUpdatedAt());
    }

    private InterviewerResponse toInterviewerResponse(AppUser user) {
        return new InterviewerResponse(
                user.getId(), user.getDisplayName(), user.getEmail(),
                user.getDepartment() == null ? null : user.getDepartment().getName());
    }

    private InterviewScorecardResponse toScorecardResponse(InterviewScorecard scorecard) {
        return new InterviewScorecardResponse(
                scorecard.getId(), scorecard.getInterviewer().getId(), scorecard.getInterviewer().getDisplayName(),
                scorecard.getTechnicalScore(), scorecard.getProblemAnalysisScore(),
                scorecard.getCommunicationScore(), scorecard.getLearningAbilityScore(),
                scorecard.getOverallComment(), scorecard.getRecommendation(), scorecard.getStatus(),
                scorecard.getSubmittedAt(), scorecard.getUnlockedAt(),
                scorecard.getUnlockedBy() == null ? null : scorecard.getUnlockedBy().getDisplayName(),
                scorecard.getUnlockReason(), scorecard.getVersion());
    }

    private void validateTimes(Instant startsAt, Instant endsAt) {
        if (!startsAt.isAfter(Instant.now())) {
            throw new BusinessException("面試開始時間必須晚於目前時間");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new BusinessException("面試結束時間必須晚於開始時間");
        }
    }

    private void transitionApplication(
            Application application, ApplicationStage nextStage, String reason, AppUser actor
    ) {
        ApplicationStage previous = application.getStage();
        application.transitionTo(nextStage);
        applicationHistories.save(new ApplicationStageHistory(application, previous, nextStage, reason, actor));
    }

    private AppUser loadActor(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("登入狀態無效"));
    }

    private Interview loadInterview(Long id) {
        return interviews.findOneById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到面試"));
    }

    private void ensureCanView(AppUser actor, Interview interview) {
        if (canAccessAll(actor)) {
            return;
        }
        if (isInterviewerOnly(actor)) {
            ensureAssigned(actor, interview);
            return;
        }
        if (!requireDepartmentId(actor).equals(
                interview.getApplication().getJobOpening().getDepartment().getId())) {
            throw new ForbiddenException("你只能查看自己部門的面試");
        }
    }

    private void ensureAssigned(AppUser actor, Interview interview) {
        if (interview.getInterviewers().stream().noneMatch(user -> user.getId().equals(actor.getId()))) {
            throw new ForbiddenException("你只能存取指派給自己的面試");
        }
    }

    private void requireHr(AppUser actor) {
        if (!canAccessAll(actor)) {
            throw new ForbiddenException("只有 HR 可以安排或管理面試");
        }
    }

    private boolean canAccessAll(AppUser actor) {
        return hasRole(actor, "HR") || hasRole(actor, "ADMIN");
    }

    private boolean isInterviewerOnly(AppUser actor) {
        return hasRole(actor, "INTERVIEWER") && !canAccessAll(actor) && !hasRole(actor, "HIRING_MANAGER");
    }

    private boolean canBeAssigned(AppUser user) {
        return hasRole(user, "INTERVIEWER") || hasRole(user, "HIRING_MANAGER");
    }

    private boolean hasRole(AppUser actor, String roleCode) {
        return actor.getRoles().stream().anyMatch(role -> role.getCode().equals(roleCode));
    }

    private Long requireDepartmentId(AppUser actor) {
        if (actor.getDepartment() == null) {
            throw new BusinessException("帳號尚未設定部門");
        }
        return actor.getDepartment().getId();
    }
}
