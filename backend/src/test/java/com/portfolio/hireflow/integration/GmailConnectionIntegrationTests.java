package com.portfolio.hireflow.integration;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@EnabledIfEnvironmentVariable(named = "HIREFLOW_SMTP_TEST", matches = "true")
class GmailConnectionIntegrationTests {

    @Test
    void authenticatesWithoutSendingAnyEmail() throws Exception {
        Path env = Files.isRegularFile(Path.of(".env")) ? Path.of(".env") : Path.of("../.env");
        Properties local = new Properties();
        try (Reader reader = Files.newBufferedReader(env, StandardCharsets.UTF_8)) {
            local.load(reader);
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(local.getProperty("MAIL_HOST"));
        sender.setPort(Integer.parseInt(local.getProperty("MAIL_PORT", "587")));
        sender.setUsername(local.getProperty("MAIL_USERNAME"));
        sender.setPassword(local.getProperty("MAIL_PASSWORD"));
        Properties smtp = sender.getJavaMailProperties();
        smtp.setProperty("mail.smtp.auth", "true");
        smtp.setProperty("mail.smtp.starttls.enable", "true");
        smtp.setProperty("mail.smtp.starttls.required", "true");
        smtp.setProperty("mail.smtp.connectiontimeout", "10000");
        smtp.setProperty("mail.smtp.timeout", "10000");
        sender.testConnection();
    }
}
