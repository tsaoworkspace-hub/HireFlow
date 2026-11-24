package com.portfolio.hireflow.audit.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.audit.dto.AuditLogResponse;
import com.portfolio.hireflow.audit.entity.AuditAction;
import com.portfolio.hireflow.audit.entity.AuditLog;
import com.portfolio.hireflow.audit.entity.AuditTargetType;
import com.portfolio.hireflow.audit.repository.AuditLogRepository;
import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.common.exception.ForbiddenException;
import com.portfolio.hireflow.common.exception.UnauthorizedException;
import com.portfolio.hireflow.common.response.PageResponse;

@Service
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogs;
    private final AppUserRepository users;

    public AuditLogService(AuditLogRepository auditLogs, AppUserRepository users) {
        this.auditLogs = auditLogs;
        this.users = users;
    }

    @Transactional
    public void record(
            AppUser actor,
            AuditAction action,
            AuditTargetType targetType,
            Long targetId,
            String details
    ) {
        auditLogs.save(new AuditLog(actor, action, targetType, targetId, details));
    }

    public PageResponse<AuditLogResponse> findAll(
            String email,
            AuditAction action,
            AuditTargetType targetType,
            Pageable pageable
    ) {
        AppUser actor = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("登入狀態無效"));
        if (actor.getRoles().stream().noneMatch(role -> role.getCode().equals("ADMIN"))) {
            throw new ForbiddenException("只有管理員可以查看全系統稽核紀錄");
        }

        Specification<AuditLog> specification = (root, query, builder) -> builder.conjunction();
        if (action != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("action"), action));
        }
        if (targetType != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("targetType"), targetType));
        }
        return PageResponse.from(auditLogs.findAll(specification, pageable).map(this::toResponse));
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(), log.getActor().getId(), log.getActor().getDisplayName(), log.getActor().getEmail(),
                log.getAction(), log.getTargetType(), log.getTargetId(), log.getOutcome(), log.getDetails(),
                log.getCreatedAt());
    }
}
