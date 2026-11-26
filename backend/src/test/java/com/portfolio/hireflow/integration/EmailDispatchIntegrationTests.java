package com.portfolio.hireflow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.portfolio.hireflow.integration.email.entity.EmailDelivery;
import com.portfolio.hireflow.integration.email.entity.EmailDeliveryStatus;
import com.portfolio.hireflow.integration.email.entity.EmailMessageType;
import com.portfolio.hireflow.integration.email.repository.EmailDeliveryRepository;
import com.portfolio.hireflow.integration.email.service.EmailDispatchService;

@SpringBootTest(properties = "app.mail.from=sender@example.test")
@ActiveProfiles("test")
class EmailDispatchIntegrationTests {

    @Autowired EmailDispatchService dispatcher;
    @Autowired EmailDeliveryRepository deliveries;
    @MockitoBean JavaMailSender sender;
    private Long deliveryId;

    @AfterEach
    void cleanup() {
        if (deliveryId != null) deliveries.deleteById(deliveryId);
    }

    @Test
    void successfulSendCommitsStatusAndDoesNotSendAgain() {
        deliveryId = pending();
        dispatcher.send(deliveryId);
        var saved = deliveries.findById(deliveryId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EmailDeliveryStatus.SENT);
        assertThat(saved.getSentAt()).isNotNull();
        assertThat(saved.getTextBody()).doesNotContain("private body");
        dispatcher.send(deliveryId);
        verify(sender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void failureCommitsRetryAndDoesNotRetryBeforeDue() {
        doThrow(new MailAuthenticationException("test failure"))
                .when(sender).send(any(SimpleMailMessage.class));
        deliveryId = pending();
        dispatcher.send(deliveryId);
        var saved = deliveries.findById(deliveryId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EmailDeliveryStatus.RETRY_WAIT);
        assertThat(saved.getRetryCount()).isEqualTo(1);
        assertThat(saved.getNextRetryAt()).isInTheFuture();
        assertThat(saved.getTextBody()).isEqualTo("private body");
        dispatcher.send(deliveryId);
        verify(sender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void thirdFailureCommitsTerminalFailedState() {
        deliveryId = pending();
        var job = deliveries.findById(deliveryId).orElseThrow();
        job.failed();
        job.failed();
        job.retry();
        deliveries.saveAndFlush(job);
        doThrow(new MailAuthenticationException("test failure"))
                .when(sender).send(any(SimpleMailMessage.class));
        dispatcher.send(deliveryId);
        var saved = deliveries.findById(deliveryId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EmailDeliveryStatus.FAILED);
        assertThat(saved.getRetryCount()).isEqualTo(3);
        assertThat(saved.getNextRetryAt()).isNull();
    }

    private Long pending() {
        return deliveries.saveAndFlush(new EmailDelivery(
                UUID.randomUUID().toString(), EmailMessageType.APPLICATION_RECEIVED,
                "TEST", 1L, "recipient@example.test", "test", "private body")).getId();
    }
}
