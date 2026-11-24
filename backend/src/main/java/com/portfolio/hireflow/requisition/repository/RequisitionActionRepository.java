package com.portfolio.hireflow.requisition.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.hireflow.requisition.entity.RequisitionAction;

public interface RequisitionActionRepository extends JpaRepository<RequisitionAction, Long> {

    @EntityGraph(attributePaths = "actor")
    List<RequisitionAction> findByRequisitionIdOrderByCreatedAtAscIdAsc(Long requisitionId);
}
