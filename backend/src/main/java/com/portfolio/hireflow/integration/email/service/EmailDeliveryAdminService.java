package com.portfolio.hireflow.integration.email.service;
import org.springframework.data.domain.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import com.portfolio.hireflow.audit.entity.*;import com.portfolio.hireflow.audit.service.AuditLogService;import com.portfolio.hireflow.auth.entity.AppUser;import com.portfolio.hireflow.auth.repository.AppUserRepository;import com.portfolio.hireflow.common.exception.*;import com.portfolio.hireflow.common.response.PageResponse;import com.portfolio.hireflow.integration.email.dto.EmailDeliveryResponse;import com.portfolio.hireflow.integration.email.entity.EmailDelivery;import com.portfolio.hireflow.integration.email.repository.EmailDeliveryRepository;
@Service @Transactional(readOnly=true)
public class EmailDeliveryAdminService{
 private final EmailDeliveryRepository repo;private final AppUserRepository users;private final AuditLogService audit;
 public EmailDeliveryAdminService(EmailDeliveryRepository repo,AppUserRepository users,AuditLogService audit){this.repo=repo;this.users=users;this.audit=audit;}
 public PageResponse<EmailDeliveryResponse> findAll(String email,Pageable pageable){requireHr(actor(email));return PageResponse.from(repo.findAllByOrderByCreatedAtDesc(pageable).map(this::response));}
 @Transactional public EmailDeliveryResponse retry(Long id,String email){AppUser a=actor(email);requireHr(a);EmailDelivery d=repo.findById(id).orElseThrow(()->new ResourceNotFoundException("找不到 Email 紀錄"));d.retry();audit.record(a,AuditAction.EMAIL_RETRY_REQUESTED,AuditTargetType.EMAIL_DELIVERY,id,null);return response(d);}
 private EmailDeliveryResponse response(EmailDelivery d){return new EmailDeliveryResponse(d.getId(),d.getMessageType(),d.getReferenceType(),d.getReferenceId(),d.getRecipientEmail(),d.getSubject(),d.getStatus(),d.getRetryCount(),d.getNextRetryAt(),d.getSentAt(),d.getLastError(),d.getCreatedAt());}
 private AppUser actor(String e){return users.findByEmailIgnoreCase(e).orElseThrow(()->new UnauthorizedException("登入狀態無效"));}
 private void requireHr(AppUser a){if(a.getRoles().stream().noneMatch(r->r.getCode().equals("HR")||r.getCode().equals("ADMIN")))throw new ForbiddenException("只有 HR 可以管理通知寄送");}
}
