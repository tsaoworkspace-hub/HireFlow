package com.portfolio.hireflow.offer.service;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.hireflow.candidate.service.PublicCandidatePortalService;
import com.portfolio.hireflow.integration.email.entity.EmailMessageType;
import com.portfolio.hireflow.integration.email.service.EmailOutboxService;
import com.portfolio.hireflow.offer.entity.JobOfferStatus;
import com.portfolio.hireflow.offer.repository.JobOfferRepository;

@Service
public class JobOfferExpiryScheduler {
    private final JobOfferRepository offers;
    private final JobOfferService offerService;
    private final PublicCandidatePortalService portal;
    private final EmailOutboxService email;
    private final String frontendBaseUrl;

    public JobOfferExpiryScheduler(JobOfferRepository offers, JobOfferService offerService,
            PublicCandidatePortalService portal, EmailOutboxService email,
            @org.springframework.beans.factory.annotation.Value("${app.frontend-base-url:http://localhost:5173}")
            String frontendBaseUrl) {
        this.offers = offers;
        this.offerService = offerService;
        this.portal = portal;
        this.email = email;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Scheduled(fixedDelayString = "${app.offer.expiry-check-delay-ms:3600000}")
    @Transactional
    public void process() {
        Instant now = Instant.now();
        offers.findByStatusAndExpiresAtBefore(JobOfferStatus.SENT, now).forEach(offerService::expire);
        offers.findByStatusAndExpiresAtBetween(JobOfferStatus.SENT, now, now.plusSeconds(86_400))
                .forEach(offer -> {
                    String key = "offer-expiring-" + offer.getId();
                    if (email.isQueued(key)) return;
                    String link = frontendBaseUrl.replaceAll("/+$", "") + portal.issueAccessPath(offer.getApplication());
                    email.queue(key, EmailMessageType.OFFER_EXPIRING, "JOB_OFFER", offer.getId(),
                            offer.getApplication().getCandidate().getEmail(), "HireFlow｜Offer 即將到期",
                            "你的 Offer 將於 " + offer.getExpiresAt() + " 到期，請儘快回覆：\n" + link);
                });
    }
}
