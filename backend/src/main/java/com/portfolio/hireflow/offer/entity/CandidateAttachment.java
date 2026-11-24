package com.portfolio.hireflow.offer.entity;

import java.time.Instant;

import com.portfolio.hireflow.candidate.entity.Application;
import jakarta.persistence.*;

@Entity
@Table(name = "candidate_attachments")
public class CandidateAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private Application application;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "uploaded_by_type", nullable = false, length = 20)
    private String uploadedByType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CandidateAttachment() {}

    public CandidateAttachment(Application application, String originalFilename, String storageKey,
            String contentType, long fileSize, String uploadedByType) {
        this.application = application;
        this.originalFilename = originalFilename;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedByType = uploadedByType;
        this.createdAt = Instant.now();
    }

    public void deactivate() { active = false; }
    public Long getId() { return id; }
    public Application getApplication() { return application; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public boolean isActive() { return active; }
    public String getUploadedByType() { return uploadedByType; }
    public Instant getCreatedAt() { return createdAt; }
}
