package com.portfolio.hireflow.candidate.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.portfolio.hireflow.candidate.entity.CandidateProfile;
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {
    Optional<CandidateProfile> findByCandidateId(Long candidateId);
}
