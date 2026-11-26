package com.portfolio.hireflow.integration.calendar.service;

import com.portfolio.hireflow.integration.calendar.entity.CalendarSyncJob;
import com.portfolio.hireflow.integration.calendar.entity.CalendarSyncOperation;
import com.portfolio.hireflow.integration.calendar.entity.CalendarSyncStatus;
import com.portfolio.hireflow.integration.calendar.entity.GoogleCalendarEventLink;
import com.portfolio.hireflow.integration.calendar.repository.CalendarSyncJobRepository;
import com.portfolio.hireflow.integration.calendar.repository.GoogleCalendarEventLinkRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CalendarSyncDispatchService {

  private static final Logger log = LoggerFactory.getLogger(CalendarSyncDispatchService.class);
  private final CalendarSyncJobRepository jobs;
  private final GoogleCalendarEventLinkRepository links;
  private final ObjectProvider<GoogleCalendarApiService> apis;
  private final TransactionTemplate transaction;

  public CalendarSyncDispatchService(
      CalendarSyncJobRepository jobs,
      GoogleCalendarEventLinkRepository links,
      ObjectProvider<GoogleCalendarApiService> apis,
      PlatformTransactionManager transactionManager) {
    this.jobs = jobs;
    this.links = links;
    this.apis = apis;
    transaction = new TransactionTemplate(transactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Scheduled(fixedDelayString = "${app.google-calendar.dispatch-delay-ms:60000}")
  public void dispatch() {
    if (apis.getIfAvailable() == null) return;
    jobs.findDue(Instant.now(), PageRequest.of(0, 20))
        .forEach(
            job -> {
              try {
                sync(job.getId());
              } catch (Exception exception) {
                log.warn("Calendar sync job {} failed", job.getId(), exception);
              }
            });
  }

  public void sync(Long id) {
    transaction.executeWithoutResult(
        status -> {
          CalendarSyncJob job = jobs.findForSync(id).orElse(null);
          if (job == null || !isDue(job)) return;
          GoogleCalendarApiService api = apis.getIfAvailable();
          if (api == null) return;
          try {
            var existing =
                links.findByInterviewIdAndUserId(job.getInterview().getId(), job.getUser().getId());
            if (job.getOperation() == CalendarSyncOperation.DELETE) {
              if (existing.isPresent()) {
                api.delete(job.getUser().getId(), existing.get().getGoogleEventId());
                links.delete(existing.get());
              }
            } else if (existing.isPresent()) {
              api.update(
                  job.getUser().getId(), existing.get().getGoogleEventId(), job.getInterview());
              existing.get().synced();
            } else {
              String eventId = api.create(job.getUser().getId(), job.getInterview());
              links.save(new GoogleCalendarEventLink(job.getInterview(), job.getUser(), eventId));
            }
            job.completed();
          } catch (Exception exception) {
            log.warn("Google Calendar sync job {} failed", id, exception);
            job.failed();
          }
        });
  }

  private boolean isDue(CalendarSyncJob job) {
    return job.getStatus() == CalendarSyncStatus.PENDING
        || (job.getStatus() == CalendarSyncStatus.RETRY_WAIT
            && job.getNextRetryAt() != null
            && !job.getNextRetryAt().isAfter(Instant.now()));
  }
}
