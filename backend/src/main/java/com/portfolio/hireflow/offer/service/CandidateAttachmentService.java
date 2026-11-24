package com.portfolio.hireflow.offer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.portfolio.hireflow.audit.entity.AuditAction;
import com.portfolio.hireflow.audit.entity.AuditTargetType;
import com.portfolio.hireflow.audit.service.AuditLogService;
import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.candidate.entity.Application;
import com.portfolio.hireflow.candidate.repository.ApplicationRepository;
import com.portfolio.hireflow.candidate.service.PublicCandidatePortalService;
import com.portfolio.hireflow.common.exception.BusinessException;
import com.portfolio.hireflow.common.exception.ForbiddenException;
import com.portfolio.hireflow.common.exception.ResourceNotFoundException;
import com.portfolio.hireflow.common.exception.UnauthorizedException;
import com.portfolio.hireflow.offer.dto.AttachmentDownload;
import com.portfolio.hireflow.offer.dto.CandidateAttachmentResponse;
import com.portfolio.hireflow.offer.entity.CandidateAttachment;
import com.portfolio.hireflow.offer.repository.CandidateAttachmentRepository;

@Service
@Transactional(readOnly = true)
public class CandidateAttachmentService {

    private static final byte[] PDF_HEADER = {'%', 'P', 'D', 'F', '-'};

    private final CandidateAttachmentRepository attachments;
    private final ApplicationRepository applications;
    private final AppUserRepository users;
    private final PublicCandidatePortalService portal;
    private final AuditLogService audit;
    private final Path storageRoot;
    private final long maxBytes;

    public CandidateAttachmentService(
            CandidateAttachmentRepository attachments,
            ApplicationRepository applications,
            AppUserRepository users,
            PublicCandidatePortalService portal,
            AuditLogService audit,
            @Value("${app.storage.resume-directory:./data/resumes}") String storageDirectory,
            @Value("${app.storage.resume-max-bytes:5242880}") long maxBytes
    ) {
        this.attachments = attachments;
        this.applications = applications;
        this.users = users;
        this.portal = portal;
        this.audit = audit;
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    public List<CandidateAttachmentResponse> findForApplication(Long applicationId, String email) {
        AppUser actor = actor(email);
        Application application = application(applicationId);
        ensureCanView(actor, application);
        return attachments.findByApplicationIdAndActiveTrueOrderByCreatedAtDesc(applicationId)
                .stream().map(this::toResponse).toList();
    }

    public List<CandidateAttachmentResponse> findPublic(UUID publicId, String token) {
        Application application = portal.requireAccessibleApplication(publicId, token);
        return attachments.findByApplicationIdAndActiveTrueOrderByCreatedAtDesc(application.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public CandidateAttachmentResponse uploadPublic(UUID publicId, String token, MultipartFile file) {
        Application application = portal.requireAccessibleApplication(publicId, token);
        byte[] content = validateAndRead(file);
        String storageKey = UUID.randomUUID() + ".pdf";
        Path target = resolveStorageKey(storageKey);

        try {
            Files.createDirectories(storageRoot);
            Files.write(target, content, StandardOpenOption.CREATE_NEW);

            attachments.findByApplicationIdAndActiveTrueOrderByCreatedAtDesc(application.getId())
                    .forEach(CandidateAttachment::deactivate);
            CandidateAttachment saved = attachments.save(new CandidateAttachment(
                    application,
                    safeFilename(file.getOriginalFilename()),
                    storageKey,
                    "application/pdf",
                    content.length,
                    "CANDIDATE"
            ));
            audit.record(application.getCreatedBy(), AuditAction.CANDIDATE_ATTACHMENT_UPLOADED,
                    AuditTargetType.CANDIDATE_ATTACHMENT, saved.getId(),
                    "Candidate portal upload for application #" + application.getId());
            return toResponse(saved);
        } catch (IOException exception) {
            throw new BusinessException("履歷檔案儲存失敗，請稍後再試");
        } catch (RuntimeException exception) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw exception;
        }
    }

    @Transactional
    public AttachmentDownload download(Long attachmentId, String email) {
        AppUser actor = actor(email);
        CandidateAttachment attachment = attachments.findOneById(attachmentId)
                .filter(CandidateAttachment::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("找不到履歷附件"));
        ensureCanView(actor, attachment.getApplication());
        Path file = resolveStorageKey(attachment.getStorageKey());
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("履歷檔案不存在");
        }
        audit.record(actor, AuditAction.CANDIDATE_ATTACHMENT_DOWNLOADED,
                AuditTargetType.CANDIDATE_ATTACHMENT, attachment.getId(), null);
        return new AttachmentDownload(new FileSystemResource(file), attachment.getOriginalFilename(),
                attachment.getContentType(), attachment.getFileSize());
    }

    private byte[] validateAndRead(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("請選擇 PDF 履歷");
        if (file.getSize() > maxBytes) throw new BusinessException("PDF 履歷不可超過 5 MB");
        try {
            byte[] content = file.getBytes();
            if (content.length < PDF_HEADER.length) throw new BusinessException("檔案不是有效的 PDF");
            for (int i = 0; i < PDF_HEADER.length; i++) {
                if (content[i] != PDF_HEADER[i]) throw new BusinessException("只接受 PDF 格式的履歷");
            }
            return content;
        } catch (IOException exception) {
            throw new BusinessException("無法讀取上傳的履歷檔案");
        }
    }

    private Path resolveStorageKey(String storageKey) {
        Path resolved = storageRoot.resolve(storageKey).normalize();
        if (!resolved.startsWith(storageRoot)) throw new BusinessException("不合法的檔案路徑");
        return resolved;
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "resume.pdf";
        String normalized = filename.replace('\\', '/');
        String safe = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\x00-\\x1F\\x7F]", "_");
        if (safe.isBlank()) safe = "resume.pdf";
        if (safe.length() > 250) safe = safe.substring(safe.length() - 250);
        return safe.toLowerCase().endsWith(".pdf") ? safe : safe + ".pdf";
    }

    private CandidateAttachmentResponse toResponse(CandidateAttachment attachment) {
        return new CandidateAttachmentResponse(attachment.getId(), attachment.getApplication().getId(),
                attachment.getOriginalFilename(), attachment.getContentType(), attachment.getFileSize(),
                attachment.getUploadedByType(), attachment.getCreatedAt());
    }

    private Application application(Long id) {
        return applications.findOneById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到應徵紀錄"));
    }

    private AppUser actor(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("登入狀態無效"));
    }

    private void ensureCanView(AppUser actor, Application application) {
        if (hasRole(actor, "ADMIN") || hasRole(actor, "HR")) return;
        if (actor.getDepartment() == null || !actor.getDepartment().getId()
                .equals(application.getJobOpening().getDepartment().getId())) {
            throw new ForbiddenException("你只能查看自己部門的履歷");
        }
    }

    private boolean hasRole(AppUser actor, String role) {
        return actor.getRoles().stream().anyMatch(item -> item.getCode().equals(role));
    }
}
