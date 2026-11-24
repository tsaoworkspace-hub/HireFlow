package com.portfolio.hireflow.offer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.hireflow.offer.entity.CandidateAttachment;

public interface CandidateAttachmentRepository extends JpaRepository<CandidateAttachment, Long> {
    @EntityGraph(attributePaths = {
            "application", "application.candidate", "application.jobOpening",
            "application.jobOpening.department"
    })
    Optional<CandidateAttachment> findOneById(Long id);

    List<CandidateAttachment> findByApplicationIdAndActiveTrueOrderByCreatedAtDesc(Long applicationId);
    Optional<CandidateAttachment> findFirstByApplicationIdAndActiveTrueOrderByCreatedAtDesc(Long applicationId);
}
