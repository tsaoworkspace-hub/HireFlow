package com.portfolio.hireflow.integration.email.service;
import java.time.Instant;
import org.slf4j.*;import org.springframework.beans.factory.ObjectProvider;import org.springframework.beans.factory.annotation.Value;import org.springframework.data.domain.PageRequest;import org.springframework.mail.SimpleMailMessage;import org.springframework.mail.javamail.JavaMailSender;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;
import com.portfolio.hireflow.integration.email.entity.EmailDelivery;import com.portfolio.hireflow.integration.email.repository.EmailDeliveryRepository;
@Service
public class EmailDispatchService{
 private static final Logger log=LoggerFactory.getLogger(EmailDispatchService.class);private final EmailDeliveryRepository repository;private final ObjectProvider<JavaMailSender> senders;private final boolean enabled;private final String from;
 public EmailDispatchService(EmailDeliveryRepository repository,ObjectProvider<JavaMailSender> senders,@Value("${app.mail.enabled:false}") boolean enabled,@Value("${app.mail.from:}") String from){this.repository=repository;this.senders=senders;this.enabled=enabled;this.from=from;}
 @Scheduled(fixedDelayString="${app.mail.dispatch-delay-ms:60000}") public void dispatchDue(){if(!enabled)return;repository.findDue(Instant.now(),PageRequest.of(0,20)).forEach(job->{try{send(job.getId());}catch(Exception e){log.warn("Email job {} dispatch failed",job.getId(),e);}});}
 @Transactional(propagation=Propagation.REQUIRES_NEW) public void send(Long id){EmailDelivery job=repository.findById(id).orElse(null);if(job==null)return;try{JavaMailSender sender=senders.getIfAvailable();if(sender==null||from.isBlank())throw new IllegalStateException("Mail sender 尚未設定");SimpleMailMessage m=new SimpleMailMessage();m.setFrom(from);m.setTo(job.getRecipientEmail());m.setSubject(job.getSubject().replaceAll("[\\r\\n]+"," "));m.setText(job.getTextBody());sender.send(m);job.sent();}catch(Exception e){log.warn("Email delivery {} failed",id,e);job.failed();}}
}
