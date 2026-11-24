package com.portfolio.hireflow.integration.email.service;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import com.portfolio.hireflow.integration.email.entity.*;import com.portfolio.hireflow.integration.email.repository.EmailDeliveryRepository;
@Service
public class EmailOutboxService{
 private final EmailDeliveryRepository repository; public EmailOutboxService(EmailDeliveryRepository repository){this.repository=repository;}
 public boolean isQueued(String key){return repository.existsByDeduplicationKey(key);}
 @Transactional public void queue(String key,EmailMessageType type,String refType,Long refId,String recipient,String subject,String body){if(!repository.existsByDeduplicationKey(key))repository.save(new EmailDelivery(key,type,refType,refId,recipient,subject,body));}
}
