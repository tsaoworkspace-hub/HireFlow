package com.portfolio.hireflow.interview.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portfolio.hireflow.interview.entity.Interview;
import com.portfolio.hireflow.interview.entity.InterviewStatus;

public interface InterviewRepository extends JpaRepository<Interview, Long>, JpaSpecificationExecutor<Interview> {

    @EntityGraph(attributePaths = {
            "application", "application.candidate", "application.jobOpening",
            "application.jobOpening.department", "interviewers", "createdBy"
    })
    Optional<Interview> findOneById(Long id);

    @Override
    @EntityGraph(attributePaths = {
            "application", "application.candidate", "application.jobOpening",
            "application.jobOpening.department"
    })
    Page<Interview> findAll(Specification<Interview> specification, Pageable pageable);

    @Query("""
            select count(i) from Interview i join i.interviewers interviewer
            where interviewer.id = :interviewerId
              and i.status <> :cancelled
              and i.startsAt < :endsAt
              and i.endsAt > :startsAt
            """)
    long countConflicts(
            @Param("interviewerId") Long interviewerId,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt,
            @Param("cancelled") InterviewStatus cancelled
    );

    @Query("""
            select count(i) from Interview i join i.interviewers interviewer
            where interviewer.id = :interviewerId and i.id <> :excludedId
              and i.status <> :cancelled and i.startsAt < :endsAt and i.endsAt > :startsAt
            """)
    long countConflictsExcluding(
            @Param("interviewerId") Long interviewerId, @Param("excludedId") Long excludedId,
            @Param("startsAt") Instant startsAt, @Param("endsAt") Instant endsAt,
            @Param("cancelled") InterviewStatus cancelled);

    long countByApplicationIdAndStatus(Long applicationId, InterviewStatus status);

    @EntityGraph(attributePaths = {
            "application", "application.candidate", "application.jobOpening", "interviewers", "createdBy"
    })
    @Query("select distinct i from Interview i left join i.interviewers interviewer where i.status=:status and i.startsAt>=:now and (i.createdBy.id=:userId or interviewer.id=:userId)")
    List<Interview> findUpcomingForUser(
            @Param("userId") Long userId, @Param("now") Instant now, @Param("status") InterviewStatus status);
}
