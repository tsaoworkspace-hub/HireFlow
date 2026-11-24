package com.portfolio.hireflow.candidate.repository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import com.portfolio.hireflow.candidate.entity.Application;
public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {
    boolean existsByCandidateIdAndJobOpeningId(Long candidateId, Long openingId);
    long countByJobOpeningId(Long openingId);
    boolean existsByCandidateIdAndJobOpeningDepartmentId(Long candidateId, Long departmentId);
    @EntityGraph(attributePaths={"candidate","jobOpening","jobOpening.department"}) Optional<Application> findOneById(Long id);
    @EntityGraph(attributePaths={"candidate","jobOpening","jobOpening.department","jobOpening.requisition"})
    Optional<Application> findByPublicId(UUID publicId);
    @Override @EntityGraph(attributePaths={"candidate","jobOpening","jobOpening.department"})
    Page<Application> findAll(Specification<Application> spec, Pageable pageable);
}
