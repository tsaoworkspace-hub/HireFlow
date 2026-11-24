package com.portfolio.hireflow.requisition.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Profile;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.auth.repository.DepartmentRepository;
import com.portfolio.hireflow.requisition.entity.JobOpening;
import com.portfolio.hireflow.requisition.entity.JobRequisition;
import com.portfolio.hireflow.requisition.repository.JobOpeningRepository;
import com.portfolio.hireflow.requisition.repository.JobRequisitionRepository;

@Component
@Order(20)
@Profile("!test")
@ConditionalOnProperty(name = "app.demo-accounts.enabled", havingValue = "true", matchIfMissing = true)
public class DemoRecruitmentDataInitializer implements ApplicationRunner {
    private final JobOpeningRepository openings;
    private final JobRequisitionRepository requisitions;
    private final AppUserRepository users;
    private final DepartmentRepository departments;

    public DemoRecruitmentDataInitializer(JobOpeningRepository openings, JobRequisitionRepository requisitions,
            AppUserRepository users, DepartmentRepository departments) {
        this.openings = openings; this.requisitions = requisitions; this.users = users; this.departments = departments;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (openings.count() > 0) return;
        var hr = users.findByEmailIgnoreCase("hr@hireflow.local").orElseThrow();
        var engineering = departments.findByName("Engineering").orElseThrow();
        JobRequisition requisition = new JobRequisition(engineering, hr);
        requisition.update(engineering, "Backend Engineer", 2,
                "負責 HireFlow 核心招募流程與企業整合功能。",
                "Java、Spring Boot、REST API、SQL；具 Vue 經驗加分。",
                new BigDecimal("65000"), new BigDecimal("95000"), LocalDate.now().plusMonths(2));
        requisition.submit();
        requisition.approve();
        requisitions.save(requisition);
        openings.save(new JobOpening(requisition, hr));
    }
}
