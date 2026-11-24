package com.portfolio.hireflow.candidate.repository;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.portfolio.hireflow.candidate.entity.Candidate;
public interface CandidateRepository extends JpaRepository<Candidate, Long>, JpaSpecificationExecutor<Candidate> {
    Optional<Candidate> findByEmailIgnoreCase(String email);
    @Query("select c from Candidate c where lower(c.fullName) like lower(concat('%',:keyword,'%')) or lower(c.email) like lower(concat('%',:keyword,'%')) or lower(c.skills) like lower(concat('%',:keyword,'%'))")
    Page<Candidate> searchAll(@Param("keyword") String keyword, Pageable pageable);
    @Query("select distinct c from Candidate c join Application a on a.candidate=c where a.jobOpening.department.id=:departmentId")
    Page<Candidate> searchByDepartment(@Param("departmentId") Long departmentId, Pageable pageable);
    @Query("select distinct c from Candidate c join Application a on a.candidate=c where a.jobOpening.department.id=:departmentId and (lower(c.fullName) like lower(concat('%',:keyword,'%')) or lower(c.email) like lower(concat('%',:keyword,'%')) or lower(c.skills) like lower(concat('%',:keyword,'%')))")
    Page<Candidate> searchByDepartment(@Param("departmentId") Long departmentId,@Param("keyword") String keyword,Pageable pageable);
}
