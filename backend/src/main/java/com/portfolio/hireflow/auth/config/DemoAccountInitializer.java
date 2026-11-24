package com.portfolio.hireflow.auth.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.Order;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.auth.entity.Department;
import com.portfolio.hireflow.auth.entity.Role;
import com.portfolio.hireflow.auth.repository.AppUserRepository;
import com.portfolio.hireflow.auth.repository.DepartmentRepository;
import com.portfolio.hireflow.auth.repository.RoleRepository;

@Component
@Order(10)
@ConditionalOnProperty(name = "app.demo-accounts.enabled", havingValue = "true", matchIfMissing = true)
public class DemoAccountInitializer implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "HireFlow123!";

    private final AppUserRepository appUserRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoAccountInitializer(
            AppUserRepository appUserRepository,
            DepartmentRepository departmentRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List.of("ADMIN", "HR", "HIRING_MANAGER", "INTERVIEWER").forEach(this::role);
        Department hr = department("Human Resources");
        Department engineering = department("Engineering");

        List<DemoAccount> accounts = List.of(
                new DemoAccount("admin@hireflow.local", "系統管理員", hr, "ADMIN"),
                new DemoAccount("hr@hireflow.local", "HR 專員", hr, "HR"),
                new DemoAccount("manager@hireflow.local", "研發主管", engineering, "HIRING_MANAGER"),
                new DemoAccount("interviewer@hireflow.local", "技術面試官", engineering, "INTERVIEWER")
        );

        accounts.forEach(this::createIfMissing);
    }

    private Department department(String name) {
        return departmentRepository.findByName(name)
                .orElseGet(() -> departmentRepository.save(new Department(name)));
    }

    private void createIfMissing(DemoAccount account) {
        if (appUserRepository.findByEmailIgnoreCase(account.email()).isPresent()) {
            return;
        }
        Role role = role(account.role());
        appUserRepository.save(new AppUser(
                account.email(),
                account.displayName(),
                passwordEncoder.encode(DEMO_PASSWORD),
                account.department(),
                role
        ));
    }

    private Role role(String code) {
        return roleRepository.findByCode(code)
                .orElseGet(() -> roleRepository.save(new Role(code)));
    }

    private record DemoAccount(String email, String displayName, Department department, String role) {
    }
}
