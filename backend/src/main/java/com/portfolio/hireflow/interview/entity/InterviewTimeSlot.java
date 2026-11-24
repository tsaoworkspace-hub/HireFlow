package com.portfolio.hireflow.interview.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.entity.Department;
import com.portfolio.hireflow.candidate.entity.Application;
import jakarta.persistence.*;

@Entity @Table(name="interview_time_slots")
public class InterviewTimeSlot {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="application_id") private Application application;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="department_id") private Department department;
    @Column(name="round_number",nullable=false) private int roundNumber;
    @Column(name="starts_at",nullable=false) private Instant startsAt;
    @Column(name="ends_at",nullable=false) private Instant endsAt;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private InterviewFormat format;
    @Column(length=255) private String location;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private InterviewTimeSlotStatus status=InterviewTimeSlotStatus.AVAILABLE;
    @ManyToMany @JoinTable(name="interview_time_slot_interviewers",joinColumns=@JoinColumn(name="slot_id"),inverseJoinColumns=@JoinColumn(name="interviewer_id"))
    private Set<AppUser> interviewers=new HashSet<>();
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="proposed_by_id") private AppUser proposedBy;
    @Column(name="selected_at") private Instant selectedAt;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="interview_id",unique=true) private Interview interview;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Version private long version;
    protected InterviewTimeSlot() {}
    public InterviewTimeSlot(Application application,int roundNumber,Instant startsAt,Instant endsAt,InterviewFormat format,String location,Set<AppUser> interviewers,AppUser proposedBy){
        this.application=application;this.department=application.getJobOpening().getDepartment();this.roundNumber=roundNumber;
        this.startsAt=startsAt;this.endsAt=endsAt;this.format=format;this.location=normalize(location);
        this.interviewers.addAll(interviewers);this.proposedBy=proposedBy;this.createdAt=Instant.now();
    }
    public void select(Interview interview){this.status=InterviewTimeSlotStatus.SELECTED;this.interview=interview;this.selectedAt=Instant.now();}
    public void withdraw(){if(status==InterviewTimeSlotStatus.AVAILABLE)this.status=InterviewTimeSlotStatus.WITHDRAWN;}
    private static String normalize(String v){return v==null||v.isBlank()?null:v.trim();}
    public Long getId(){return id;} public Application getApplication(){return application;} public Department getDepartment(){return department;}
    public int getRoundNumber(){return roundNumber;} public Instant getStartsAt(){return startsAt;} public Instant getEndsAt(){return endsAt;}
    public InterviewFormat getFormat(){return format;} public String getLocation(){return location;} public InterviewTimeSlotStatus getStatus(){return status;}
    public Set<AppUser> getInterviewers(){return interviewers;} public AppUser getProposedBy(){return proposedBy;} public Instant getSelectedAt(){return selectedAt;}
    public Interview getInterview(){return interview;} public Instant getCreatedAt(){return createdAt;} public long getVersion(){return version;}
}
