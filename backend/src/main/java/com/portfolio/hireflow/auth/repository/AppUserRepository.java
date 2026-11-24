package com.portfolio.hireflow.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.portfolio.hireflow.auth.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<AppUser> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"department", "roles"})
    @Query("select distinct user from AppUser user join user.roles role where role.code in ('INTERVIEWER', 'HIRING_MANAGER') and user.enabled = true order by user.displayName")
    java.util.List<AppUser> findAssignableInterviewers();
}
