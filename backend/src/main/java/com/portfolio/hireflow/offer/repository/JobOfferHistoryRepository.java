package com.portfolio.hireflow.offer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.hireflow.offer.entity.JobOfferHistory;

public interface JobOfferHistoryRepository extends JpaRepository<JobOfferHistory, Long> {
    @EntityGraph(attributePaths = "actor")
    List<JobOfferHistory> findByOfferIdOrderByCreatedAtAscIdAsc(Long offerId);
}
