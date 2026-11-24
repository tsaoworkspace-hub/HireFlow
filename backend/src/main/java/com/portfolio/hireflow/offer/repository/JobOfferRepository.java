package com.portfolio.hireflow.offer.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.hireflow.offer.entity.JobOffer;
import com.portfolio.hireflow.offer.entity.JobOfferStatus;

public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {
    boolean existsByApplicationId(Long applicationId);

    @EntityGraph(attributePaths = {
            "application", "application.candidate", "application.jobOpening",
            "application.jobOpening.department", "createdBy", "approvedBy"
    })
    Optional<JobOffer> findOneById(Long id);

    @EntityGraph(attributePaths = {
            "application", "application.candidate", "application.jobOpening",
            "application.jobOpening.department", "createdBy", "approvedBy"
    })
    Optional<JobOffer> findByApplicationId(Long applicationId);

    @EntityGraph(attributePaths = {
            "application", "application.candidate", "application.jobOpening",
            "application.jobOpening.department"
    })
    Optional<JobOffer> findByApplicationPublicId(UUID publicId);

    @EntityGraph(attributePaths = {
            "application", "application.candidate", "application.jobOpening",
            "application.jobOpening.department", "createdBy", "approvedBy"
    })
    Page<JobOffer> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {
            "application", "application.candidate", "application.jobOpening",
            "application.jobOpening.department", "createdBy", "approvedBy"
    })
    Page<JobOffer> findByApplicationJobOpeningDepartmentIdOrderByUpdatedAtDesc(
            Long departmentId, Pageable pageable);

    List<JobOffer> findByStatusAndExpiresAtBefore(JobOfferStatus status, Instant time);
    List<JobOffer> findByStatusAndExpiresAtBetween(JobOfferStatus status, Instant from, Instant to);
}
