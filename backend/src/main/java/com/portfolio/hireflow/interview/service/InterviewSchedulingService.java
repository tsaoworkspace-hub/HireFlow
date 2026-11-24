package com.portfolio.hireflow.interview.service;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.portfolio.hireflow.audit.entity.*;
import com.portfolio.hireflow.audit.service.AuditLogService;
import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.candidate.entity.*;
import com.portfolio.hireflow.candidate.repository.*;
import com.portfolio.hireflow.candidate.service.PublicCandidatePortalService;
import com.portfolio.hireflow.common.exception.*;
import com.portfolio.hireflow.interview.dto.*;
import com.portfolio.hireflow.interview.entity.*;
import com.portfolio.hireflow.interview.repository.*;
import com.portfolio.hireflow.requisition.entity.JobOpeningStatus;
import com.portfolio.hireflow.integration.email.entity.EmailMessageType;
import com.portfolio.hireflow.integration.email.service.EmailOutboxService;
import com.portfolio.hireflow.integration.calendar.service.CalendarSyncQueueService;

@Service @Transactional(readOnly=true)
public class InterviewSchedulingService {
    private static final Set<ApplicationStage> ALLOWED=Set.of(ApplicationStage.SCREENING,ApplicationStage.INTERVIEW_SCHEDULED,ApplicationStage.INTERVIEWED);
    private final InterviewTimeSlotRepository slots; private final InterviewRepository interviews;
    private final ApplicationRepository applications; private final ApplicationStageHistoryRepository histories;
    private final AppUserRepository users; private final PublicCandidatePortalService portal;
    private final AuditLogService audit;
    private final EmailOutboxService emailOutbox;
    private final CalendarSyncQueueService calendarSync;
    public InterviewSchedulingService(InterviewTimeSlotRepository slots,InterviewRepository interviews,ApplicationRepository applications,
      ApplicationStageHistoryRepository histories,AppUserRepository users,PublicCandidatePortalService portal,AuditLogService audit,EmailOutboxService emailOutbox,CalendarSyncQueueService calendarSync){
      this.slots=slots;this.interviews=interviews;this.applications=applications;this.histories=histories;this.users=users;this.portal=portal;this.audit=audit;this.emailOutbox=emailOutbox;this.calendarSync=calendarSync;
    }

    public List<InterviewTimeSlotResponse> findForApplication(Long applicationId,String email){
      AppUser actor=actor(email); Application application=application(applicationId); ensureView(actor,application);
      return slots.findByApplicationIdAndStatusOrderByStartsAt(applicationId,InterviewTimeSlotStatus.AVAILABLE).stream().map(this::response).toList();
    }
    @Transactional public InterviewTimeSlotResponse propose(String email,CreateInterviewTimeSlotRequest request){
      AppUser actor=actor(email); requireHr(actor); validateTimes(request.startsAt(),request.endsAt());
      Application application=application(request.applicationId());
      if(application.getJobOpening().getStatus()!=JobOpeningStatus.OPEN||!ALLOWED.contains(application.getStage())) throw new BusinessException("目前應徵紀錄不能提供面試時段");
      Set<AppUser> assigned=new HashSet<>(users.findAllById(request.interviewerIds()));
      if(assigned.size()!=request.interviewerIds().size()) throw new ResourceNotFoundException("部分面試官不存在");
      assigned.forEach(user->{if(!assignable(user))throw new BusinessException(user.getDisplayName()+" 不是可指派面試官");});
      InterviewTimeSlot slot=slots.save(new InterviewTimeSlot(application,request.roundNumber(),request.startsAt(),request.endsAt(),request.format(),request.location(),assigned,actor));
      audit.record(actor,AuditAction.INTERVIEW_SLOT_PROPOSED,AuditTargetType.INTERVIEW_SLOT,slot.getId(),"Application #"+application.getId());
      emailOutbox.queue("interview-slot-"+slot.getId(),EmailMessageType.INTERVIEW_SLOT_INVITATION,"INTERVIEW_SLOT",slot.getId(),application.getCandidate().getEmail(),"HireFlow｜請選擇面試時間",application.getCandidate().getFullName()+" 您好：\n\n「"+application.getJobOpening().getJobTitle()+"」已有新的面試時段可供選擇，請使用先前收到的應徵進度專屬連結進行確認。");
      return response(slot);
    }
    @Transactional public void withdraw(Long id,String email){
      AppUser actor=actor(email);requireHr(actor);InterviewTimeSlot slot=slot(id);
      if(slot.getStatus()!=InterviewTimeSlotStatus.AVAILABLE)throw new BusinessException("只有可選時段能撤回");slot.withdraw();
    }
    public List<InterviewTimeSlotResponse> findPublic(UUID publicId,String token){
      Application application=portal.requireAccessibleApplication(publicId,token);
      return slots.findByApplicationIdAndStatusOrderByStartsAt(application.getId(),InterviewTimeSlotStatus.AVAILABLE).stream()
        .filter(s->s.getStartsAt().isAfter(Instant.now())).map(this::response).toList();
    }
    @Transactional public InterviewTimeSlotResponse selectPublic(UUID publicId,Long slotId,String token){
      Application application=portal.requireAccessibleApplication(publicId,token);InterviewTimeSlot slot=slot(slotId);
      if(!slot.getApplication().getId().equals(application.getId()))throw new ResourceNotFoundException("找不到可選時段");
      if(slot.getStatus()!=InterviewTimeSlotStatus.AVAILABLE||!slot.getStartsAt().isAfter(Instant.now()))throw new ConflictException("此面試時段已無法選擇");
      for(AppUser interviewer:slot.getInterviewers()) if(interviews.countConflicts(interviewer.getId(),slot.getStartsAt(),slot.getEndsAt(),InterviewStatus.CANCELLED)>0)
        throw new ConflictException("此時段剛被其他面試占用，請選擇其他時間");
      Interview interview=interviews.save(new Interview(application,slot.getRoundNumber(),slot.getStartsAt(),slot.getEndsAt(),slot.getFormat(),slot.getLocation(),slot.getInterviewers(),slot.getProposedBy()));
      slot.select(interview);
      slots.findByApplicationIdAndStatusOrderByStartsAt(application.getId(),InterviewTimeSlotStatus.AVAILABLE).stream().filter(other->!other.getId().equals(slotId)).forEach(InterviewTimeSlot::withdraw);
      if(application.getStage()==ApplicationStage.SCREENING){ApplicationStage from=application.getStage();application.transitionTo(ApplicationStage.INTERVIEW_SCHEDULED);
        histories.save(new ApplicationStageHistory(application,from,ApplicationStage.INTERVIEW_SCHEDULED,"求職者已確認面試時間",slot.getProposedBy()));}
      audit.record(slot.getProposedBy(),AuditAction.INTERVIEW_SLOT_SELECTED,AuditTargetType.INTERVIEW_SLOT,slot.getId(),"Interview #"+interview.getId());
      emailOutbox.queue("interview-confirmed-"+interview.getId(),EmailMessageType.INTERVIEW_CONFIRMED,"INTERVIEW",interview.getId(),application.getCandidate().getEmail(),"HireFlow｜面試時間已確認",application.getCandidate().getFullName()+" 您好：\n\n面試時間已確認："+slot.getStartsAt()+" ～ "+slot.getEndsAt()+"\n形式："+slot.getFormat()+"\n地點／連結："+(slot.getLocation()==null?"將另行通知":slot.getLocation()));
      calendarSync.queueUpserts(interview);
      return response(slot);
    }
    private InterviewTimeSlotResponse response(InterviewTimeSlot s){return new InterviewTimeSlotResponse(s.getId(),s.getApplication().getId(),s.getApplication().getCandidate().getFullName(),s.getApplication().getJobOpening().getJobTitle(),s.getDepartment().getName(),s.getRoundNumber(),s.getStartsAt(),s.getEndsAt(),s.getFormat(),s.getLocation(),s.getStatus(),s.getInterviewers().stream().map(u->new InterviewerResponse(u.getId(),u.getDisplayName(),u.getEmail(),u.getDepartment()==null?null:u.getDepartment().getName())).toList(),s.getInterview()==null?null:s.getInterview().getId(),s.getVersion());}
    private void validateTimes(Instant start,Instant end){if(!start.isAfter(Instant.now())||!end.isAfter(start))throw new BusinessException("面試時段必須在未來，且結束晚於開始");}
    private AppUser actor(String email){return users.findByEmailIgnoreCase(email).orElseThrow(()->new UnauthorizedException("登入狀態無效"));}
    private Application application(Long id){return applications.findOneById(id).orElseThrow(()->new ResourceNotFoundException("找不到應徵紀錄"));}
    private InterviewTimeSlot slot(Long id){return slots.findOneById(id).orElseThrow(()->new ResourceNotFoundException("找不到面試時段"));}
    private void requireHr(AppUser a){if(!has(a,"HR")&&!has(a,"ADMIN"))throw new ForbiddenException("只有 HR 可以提供面試時段");}
    private void ensureView(AppUser a,Application app){if(has(a,"HR")||has(a,"ADMIN"))return;if(a.getDepartment()==null||!a.getDepartment().getId().equals(app.getJobOpening().getDepartment().getId()))throw new ForbiddenException("你只能查看自己部門的面試時段");}
    private boolean assignable(AppUser a){return has(a,"INTERVIEWER")||has(a,"HIRING_MANAGER");}
    private boolean has(AppUser a,String role){return a.getRoles().stream().anyMatch(r->r.getCode().equals(role));}
}
