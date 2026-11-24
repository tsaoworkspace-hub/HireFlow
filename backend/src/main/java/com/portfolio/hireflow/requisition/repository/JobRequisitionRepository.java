package com.portfolio.hireflow.requisition.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import com.portfolio.hireflow.requisition.entity.JobRequisition;

public interface JobRequisitionRepository
        extends JpaRepository<JobRequisition, Long>, JpaSpecificationExecutor<JobRequisition> {

    @EntityGraph(attributePaths = {"department", "createdBy"})
    Optional<JobRequisition> findOneById(Long id);

    @Override
    @EntityGraph(attributePaths = {"department", "createdBy"})
    Page<JobRequisition> findAll(Specification<JobRequisition> specification, Pageable pageable);
}
