package com.portfolio.hireflow.integration.email.repository;

import com.portfolio.hireflow.integration.email.entity.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, Long> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from EmailDelivery e where e.id = :id")
  Optional<EmailDelivery> findForDelivery(@Param("id") Long id);

  boolean existsByDeduplicationKey(String key);

  @Query(
      "select e from EmailDelivery e where e.status='PENDING' or (e.status='RETRY_WAIT' and"
          + " e.nextRetryAt<=:now) order by e.createdAt")
  List<EmailDelivery> findDue(@Param("now") Instant now, Pageable pageable);

  Page<EmailDelivery> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
