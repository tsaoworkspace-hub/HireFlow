package com.portfolio.hireflow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.candidate.entity.Application;
import com.portfolio.hireflow.candidate.entity.Candidate;
import com.portfolio.hireflow.integration.calendar.entity.CalendarSyncJob;
import com.portfolio.hireflow.integration.calendar.entity.CalendarSyncOperation;
import com.portfolio.hireflow.integration.calendar.entity.CalendarSyncStatus;
import com.portfolio.hireflow.integration.calendar.repository.CalendarSyncJobRepository;
import com.portfolio.hireflow.integration.calendar.repository.GoogleCalendarEventLinkRepository;
import com.portfolio.hireflow.integration.calendar.service.CalendarSyncDispatchService;
import com.portfolio.hireflow.integration.calendar.service.GoogleCalendarApiService;
import com.portfolio.hireflow.interview.entity.Interview;
import com.portfolio.hireflow.interview.entity.InterviewFormat;
import com.portfolio.hireflow.requisition.entity.JobOpening;
import com.portfolio.hireflow.requisition.entity.JobRequisition;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:calendarDispatch;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CalendarDispatchIntegrationTests {

    @Autowired CalendarSyncDispatchService dispatcher;
    @Autowired CalendarSyncJobRepository jobs;
    @Autowired GoogleCalendarEventLinkRepository links;
    @Autowired AppUserRepository users;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean GoogleCalendarApiService api;

    @Test
    void createUpdateDeletePersistResultsAcrossSeparateTransactions() {
        Long id = newJob();
        when(api.create(anyLong(), any(Interview.class))).thenReturn("test-event");
        dispatcher.sync(id);
        dispatcher.sync(id);
        assertThat(jobs.findById(id).orElseThrow().getStatus()).isEqualTo(CalendarSyncStatus.COMPLETED);
        verify(api, times(1)).create(anyLong(), any(Interview.class));

        Long update = nextJob(id, CalendarSyncOperation.UPSERT);
        dispatcher.sync(update);
        assertThat(jobs.findById(update).orElseThrow().getStatus()).isEqualTo(CalendarSyncStatus.COMPLETED);
        verify(api).update(anyLong(), any(String.class), any(Interview.class));
        assertThat(links.count()).isEqualTo(1);

        Long delete = nextJob(id, CalendarSyncOperation.DELETE);
        dispatcher.sync(delete);
        assertThat(jobs.findById(delete).orElseThrow().getStatus()).isEqualTo(CalendarSyncStatus.COMPLETED);
        verify(api).delete(anyLong(), any(String.class));
        assertThat(links.count()).isZero();
    }

    @Test
    void failurePersistsRetryAndDoesNotRetryEarly() {
        Long id = newJob();
        doThrow(new IllegalStateException("simulated Google failure"))
                .when(api).create(anyLong(), any(Interview.class));
        dispatcher.sync(id);
        dispatcher.sync(id);
        var saved = jobs.findById(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(CalendarSyncStatus.RETRY_WAIT);
        assertThat(saved.getRetryCount()).isEqualTo(1);
        verify(api, times(1)).create(anyLong(), any(Interview.class));
    }

    private Long nextJob(Long previous, CalendarSyncOperation operation) {
        return new TransactionTemplate(transactions).execute(status -> {
            var prior = jobs.findById(previous).orElseThrow();
            return jobs.saveAndFlush(new CalendarSyncJob(UUID.randomUUID().toString(),
                    prior.getInterview(), prior.getUser(), operation)).getId();
        });
    }

    private Long newJob() {
        return new TransactionTemplate(transactions).execute(status -> {
            var hr = users.findByEmailIgnoreCase("hr@hireflow.local").orElseThrow();
            var requisition = new JobRequisition(hr.getDepartment(), hr);
            requisition.update(hr.getDepartment(), "Test Engineer", 1, "test", "Java",
                    null, null, LocalDate.now().plusDays(30));
            em.persist(requisition);
            var opening = new JobOpening(requisition, hr);
            em.persist(opening);
            var candidate = new Candidate("Test candidate", UUID.randomUUID() + "@example.test");
            em.persist(candidate);
            var application = new Application(candidate, opening, hr);
            em.persist(application);
            var interview = new Interview(application, 1, Instant.now().plusSeconds(86400),
                    Instant.now().plusSeconds(90000), InterviewFormat.ONLINE, "test", Set.of(hr), hr);
            em.persist(interview);
            return jobs.saveAndFlush(new CalendarSyncJob(UUID.randomUUID().toString(),
                    interview, hr, CalendarSyncOperation.UPSERT)).getId();
        });
    }
}
