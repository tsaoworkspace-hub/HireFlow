package com.portfolio.hireflow.decision.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.hireflow.decision.entity.HiringDecision;

public interface HiringDecisionRepository extends JpaRepository<HiringDecision, Long> {

    boolean existsByApplicationId(Long applicationId);

    @EntityGraph(attributePaths = "decidedBy")
    Optional<HiringDecision> findByApplicationId(Long applicationId);
}
