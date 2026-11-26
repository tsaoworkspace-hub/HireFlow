package com.portfolio.hireflow.integration.email.service;

import com.portfolio.hireflow.integration.email.entity.EmailDelivery;
import com.portfolio.hireflow.integration.email.entity.EmailDeliveryStatus;
import com.portfolio.hireflow.integration.email.repository.EmailDeliveryRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EmailDispatchService {

  private static final Logger log = LoggerFactory.getLogger(EmailDispatchService.class);
  private final EmailDeliveryRepository repository;
  private final ObjectProvider<JavaMailSender> senders;
  private final boolean enabled;
  private final String from;
  private final TransactionTemplate transaction;

  public EmailDispatchService(
      EmailDeliveryRepository repository,
      ObjectProvider<JavaMailSender> senders,
      @Value("${app.mail.enabled:false}") boolean enabled,
      @Value("${app.mail.from:}") String from,
      PlatformTransactionManager transactionManager) {
    this.repository = repository;
    this.senders = senders;
    this.enabled = enabled;
    this.from = from;
    transaction = new TransactionTemplate(transactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Scheduled(fixedDelayString = "${app.mail.dispatch-delay-ms:60000}")
  public void dispatchDue() {
    if (!enabled) return;
    repository
        .findDue(Instant.now(), PageRequest.of(0, 20))
        .forEach(
            job -> {
              try {
                send(job.getId());
              } catch (Exception exception) {
                log.warn("Email job {} dispatch failed", job.getId(), exception);
              }
            });
  }

  public void send(Long id) {
    // Explicit transaction also applies when invoked by this service's scheduler.
    transaction.executeWithoutResult(
        status -> {
          EmailDelivery job = repository.findForDelivery(id).orElse(null);
          if (job == null || !isDue(job)) return;
          try {
            JavaMailSender sender = senders.getIfAvailable();
            if (sender == null || from.isBlank()) {
              throw new IllegalStateException("Mail sender 尚未設定");
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(job.getRecipientEmail());
            message.setSubject(job.getSubject().replaceAll("[\\r\\n]+", " "));
            message.setText(job.getTextBody());
            sender.send(message);
            job.sent();
          } catch (Exception exception) {
            log.warn("Email delivery {} failed", id, exception);
            job.failed();
          }
        });
  }

  private boolean isDue(EmailDelivery job) {
    return job.getStatus() == EmailDeliveryStatus.PENDING
        || (job.getStatus() == EmailDeliveryStatus.RETRY_WAIT
            && job.getNextRetryAt() != null
            && !job.getNextRetryAt().isAfter(Instant.now()));
  }
}
