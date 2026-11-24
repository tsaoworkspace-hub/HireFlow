package com.portfolio.hireflow.interview.repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import com.portfolio.hireflow.interview.entity.*;
public interface InterviewTimeSlotRepository extends JpaRepository<InterviewTimeSlot,Long>{
    @EntityGraph(attributePaths={"application","application.candidate","application.jobOpening","application.jobOpening.department","interviewers","proposedBy"})
    Optional<InterviewTimeSlot> findOneById(Long id);
    @EntityGraph(attributePaths={"interviewers"}) List<InterviewTimeSlot> findByApplicationIdAndStatusOrderByStartsAt(Long applicationId,InterviewTimeSlotStatus status);
    @EntityGraph(attributePaths={"application","application.candidate","application.jobOpening","application.jobOpening.department","interviewers"})
    List<InterviewTimeSlot> findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(Instant from,Instant to);
}
