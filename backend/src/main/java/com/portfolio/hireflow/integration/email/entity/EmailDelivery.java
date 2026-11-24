package com.portfolio.hireflow.integration.email.entity;
import java.time.Instant;
import jakarta.persistence.*;
@Entity @Table(name="email_deliveries")
public class EmailDelivery{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="deduplication_key",nullable=false,unique=true,length=180) private String deduplicationKey;
 @Enumerated(EnumType.STRING) @Column(name="message_type",nullable=false,length=50) private EmailMessageType messageType;
 @Column(name="reference_type",nullable=false,length=40) private String referenceType;
 @Column(name="reference_id",nullable=false) private Long referenceId;
 @Column(name="recipient_email",nullable=false,length=255) private String recipientEmail;
 @Column(nullable=false,length=300) private String subject;
 @Column(name="text_body",nullable=false,length=8000) private String textBody;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private EmailDeliveryStatus status=EmailDeliveryStatus.PENDING;
 @Column(name="retry_count",nullable=false) private int retryCount;
 @Column(name="next_retry_at") private Instant nextRetryAt; @Column(name="sent_at") private Instant sentAt;
 @Column(name="last_error",length=500) private String lastError;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected EmailDelivery(){}
 public EmailDelivery(String key,EmailMessageType type,String refType,Long refId,String email,String subject,String body){this.deduplicationKey=key;this.messageType=type;this.referenceType=refType;this.referenceId=refId;this.recipientEmail=email;this.subject=subject;this.textBody=body;this.createdAt=Instant.now();this.updatedAt=createdAt;}
 public void sent(){status=EmailDeliveryStatus.SENT;sentAt=Instant.now();lastError=null;nextRetryAt=null;updatedAt=sentAt;textBody="訊息已寄出，內容已清除";}
 public void failed(){retryCount++;lastError="Email 寄送失敗，請檢查 Gmail SMTP 設定與後端日誌";updatedAt=Instant.now();if(retryCount>=3){status=EmailDeliveryStatus.FAILED;nextRetryAt=null;}else{status=EmailDeliveryStatus.RETRY_WAIT;nextRetryAt=Instant.now().plusSeconds(retryCount==1?60:300);}}
 public void retry(){status=EmailDeliveryStatus.PENDING;nextRetryAt=null;lastError=null;updatedAt=Instant.now();}
 public Long getId(){return id;} public EmailMessageType getMessageType(){return messageType;} public String getReferenceType(){return referenceType;} public Long getReferenceId(){return referenceId;} public String getRecipientEmail(){return recipientEmail;} public String getSubject(){return subject;} public String getTextBody(){return textBody;} public EmailDeliveryStatus getStatus(){return status;} public int getRetryCount(){return retryCount;} public Instant getNextRetryAt(){return nextRetryAt;} public Instant getSentAt(){return sentAt;} public String getLastError(){return lastError;} public Instant getCreatedAt(){return createdAt;}
}
