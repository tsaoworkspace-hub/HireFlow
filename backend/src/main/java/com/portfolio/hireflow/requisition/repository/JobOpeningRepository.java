package com.portfolio.hireflow.requisition.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;

import com.portfolio.hireflow.requisition.entity.JobOpening;

public interface JobOpeningRepository extends JpaRepository<JobOpening, Long> {

    boolean existsByRequisitionId(Long requisitionId);

    long countByRequisitionId(Long requisitionId);

    Optional<JobOpening> findByRequisitionId(Long requisitionId);

    @EntityGraph(attributePaths={"department","requisition"})
    List<JobOpening> findByStatusAndPublishedAtIsNotNullOrderByPublishedAtDesc(
            com.portfolio.hireflow.requisition.entity.JobOpeningStatus status);

    @EntityGraph(attributePaths={"department","requisition"})
    Optional<JobOpening> findByPublicSlugAndStatusAndPublishedAtIsNotNull(
            String publicSlug, com.portfolio.hireflow.requisition.entity.JobOpeningStatus status);

    @EntityGraph(attributePaths={"department","responsibleHr","requisition"})
    Optional<JobOpening> findOneById(Long id);

    @EntityGraph(attributePaths={"department","responsibleHr","requisition"})
    @Query("select j from JobOpening j where (:departmentId is null or j.department.id=:departmentId) and (:status is null or j.status=:status) and (:keyword is null or lower(j.jobTitle) like lower(concat('%',:keyword,'%')))")
    Page<JobOpening> search(@Param("departmentId") Long departmentId,@Param("status") com.portfolio.hireflow.requisition.entity.JobOpeningStatus status,@Param("keyword") String keyword,Pageable pageable);

    @EntityGraph(attributePaths={"department","responsibleHr","requisition"})
    @Query("select j from JobOpening j where (:departmentId is null or j.department.id=:departmentId) and (:status is null or j.status=:status)")
    Page<JobOpening> searchWithoutKeyword(@Param("departmentId") Long departmentId,@Param("status") com.portfolio.hireflow.requisition.entity.JobOpeningStatus status,Pageable pageable);
}
