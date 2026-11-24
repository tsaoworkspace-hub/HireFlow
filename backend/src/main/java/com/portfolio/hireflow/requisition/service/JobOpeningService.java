package com.portfolio.hireflow.requisition.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.candidate.repository.ApplicationRepository;
import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.common.exception.ForbiddenException;
import com.portfolio.hireflow.common.exception.ResourceNotFoundException;
import com.portfolio.hireflow.common.exception.UnauthorizedException;
import com.portfolio.hireflow.common.response.PageResponse;
import com.portfolio.hireflow.requisition.dto.JobOpeningListResponse;
import com.portfolio.hireflow.requisition.entity.JobOpening;
import com.portfolio.hireflow.requisition.entity.JobOpeningStatus;
import com.portfolio.hireflow.requisition.repository.JobOpeningRepository;

@Service
@Transactional(readOnly = true)
public class JobOpeningService {

    private final JobOpeningRepository openings;
    private final ApplicationRepository applications;
    private final AppUserRepository users;

    public JobOpeningService(JobOpeningRepository openings, ApplicationRepository applications, AppUserRepository users) {
        this.openings = openings;
        this.applications = applications;
        this.users = users;
    }

    public PageResponse<JobOpeningListResponse> findAll(
            String email, JobOpeningStatus status, String keyword, Pageable pageable
    ) {
        AppUser actor = actor(email);
        Long departmentId = canAccessAll(actor) ? null : departmentId(actor);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<JobOpening> openingsPage = normalizedKeyword == null
                ? openings.searchWithoutKeyword(departmentId, status, pageable)
                : openings.search(departmentId, status, normalizedKeyword, pageable);
        Page<JobOpeningListResponse> page = openingsPage
                .map(this::response);
        return PageResponse.from(page);
    }

    public JobOpeningListResponse findById(Long id, String email) {
        AppUser actor = actor(email);
        JobOpening opening = opening(id);
        ensureAccess(actor, opening);
        return response(opening);
    }

    @Transactional
    public JobOpeningListResponse close(Long id, String email) {
        AppUser actor = actor(email);
        if (!canAccessAll(actor)) {
            throw new ForbiddenException("只有 HR 可以關閉職缺");
        }
        JobOpening opening = opening(id);
        if (opening.getStatus() == JobOpeningStatus.CLOSED) {
            throw new BusinessException("此職缺已經關閉");
        }
        opening.close();
        return response(opening);
    }

    private JobOpeningListResponse response(JobOpening opening) {
        return new JobOpeningListResponse(
                opening.getId(), opening.getRequisition().getId(), opening.getDepartment().getId(),
                opening.getDepartment().getName(), opening.getJobTitle(), opening.getHeadcount(),
                opening.getStatus(), opening.getResponsibleHr().getDisplayName(),
                applications.countByJobOpeningId(opening.getId()), opening.getVersion(),
                opening.getCreatedAt(), opening.getClosedAt());
    }

    private AppUser actor(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new UnauthorizedException("登入狀態無效"));
    }

    private JobOpening opening(Long id) {
        return openings.findOneById(id).orElseThrow(() -> new ResourceNotFoundException("找不到職缺"));
    }

    private void ensureAccess(AppUser actor, JobOpening opening) {
        if (!canAccessAll(actor) && !departmentId(actor).equals(opening.getDepartment().getId())) {
            throw new ForbiddenException("你只能查看自己部門的職缺");
        }
    }

    private boolean canAccessAll(AppUser user) {
        return user.getRoles().stream().anyMatch(role -> role.getCode().equals("HR") || role.getCode().equals("ADMIN"));
    }

    private Long departmentId(AppUser user) {
        if (user.getDepartment() == null) {
            throw new BusinessException("帳號尚未設定部門");
        }
        return user.getDepartment().getId();
    }
}
