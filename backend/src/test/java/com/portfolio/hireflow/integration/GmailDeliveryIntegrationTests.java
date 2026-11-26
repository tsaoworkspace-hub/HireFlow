package com.portfolio.hireflow.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.portfolio.hireflow.integration.email.entity.EmailDelivery;
import com.portfolio.hireflow.integration.email.entity.EmailDeliveryStatus;
import com.portfolio.hireflow.integration.email.entity.EmailMessageType;
import com.portfolio.hireflow.integration.email.repository.EmailDeliveryRepository;
import com.portfolio.hireflow.integration.email.service.EmailDispatchService;

/** Opt-in: sends exactly one notification to an explicitly selected recipient. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:gmailDelivery;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "HIREFLOW_SMTP_SEND_TEST", matches = "true")
@EnabledIfEnvironmentVariable(named = "HIREFLOW_TEST_RECIPIENT", matches = ".+@.+")
class GmailDeliveryIntegrationTests {

    @Autowired EmailDeliveryRepository deliveries;
    @Autowired EmailDispatchService dispatcher;

    @DynamicPropertySource
    static void smtpProperties(DynamicPropertyRegistry registry) throws Exception {
        Properties local = new Properties();
        Path file = Files.isRegularFile(Path.of(".env")) ? Path.of(".env") : Path.of("../.env");
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            local.load(reader);
        }
        registry.add("spring.mail.host", () -> local.getProperty("MAIL_HOST"));
        registry.add("spring.mail.port", () -> local.getProperty("MAIL_PORT", "587"));
        registry.add("spring.mail.username", () -> local.getProperty("MAIL_USERNAME"));
        registry.add("spring.mail.password", () -> local.getProperty("MAIL_PASSWORD"));
        registry.add("app.mail.from", () -> local.getProperty("MAIL_FROM"));
    }

    @Test
    void sendsOneExplicitTestNotificationAndCommitsSentStatus() {
        var job = deliveries.saveAndFlush(new EmailDelivery(
                "smtp-check-" + UUID.randomUUID(), EmailMessageType.APPLICATION_RECEIVED,
                "SMTP_TEST", 1L, System.getenv("HIREFLOW_TEST_RECIPIENT"),
                "HireFlow 寄信功能測試",
                "這是你授權的 HireFlow 測試通知。若收到這封信，代表 Gmail SMTP 寄送成功。無需回覆或點擊連結。"));
        try {
            dispatcher.send(job.getId());
            assertThat(deliveries.findById(job.getId()).orElseThrow().getStatus())
                    .isEqualTo(EmailDeliveryStatus.SENT);
        } finally {
            deliveries.deleteById(job.getId());
        }
    }
}
