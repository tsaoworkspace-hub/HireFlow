package com.portfolio.hireflow.candidate.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.portfolio.hireflow.candidate.entity.WorkExperience;
public interface WorkExperienceRepository extends JpaRepository<WorkExperience, Long> {
    List<WorkExperience> findByCandidateIdOrderBySortOrder(Long candidateId);
    void deleteByCandidateId(Long candidateId);
}
