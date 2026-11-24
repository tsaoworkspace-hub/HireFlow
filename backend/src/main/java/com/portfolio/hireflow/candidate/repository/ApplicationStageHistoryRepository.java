package com.portfolio.hireflow.candidate.repository;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import com.portfolio.hireflow.candidate.entity.ApplicationStageHistory;
public interface ApplicationStageHistoryRepository extends JpaRepository<ApplicationStageHistory, Long> {
    @EntityGraph(attributePaths="actor") List<ApplicationStageHistory> findByApplicationIdOrderByCreatedAtAscIdAsc(Long id);
}
