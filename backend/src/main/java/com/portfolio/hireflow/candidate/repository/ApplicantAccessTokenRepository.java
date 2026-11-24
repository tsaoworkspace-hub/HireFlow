package com.portfolio.hireflow.candidate.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.portfolio.hireflow.candidate.entity.ApplicantAccessToken;
public interface ApplicantAccessTokenRepository extends JpaRepository<ApplicantAccessToken, Long> {
    Optional<ApplicantAccessToken> findByTokenHash(String tokenHash);
}
