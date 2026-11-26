package com.portfolio.hireflow.integration.calendar.entity;

import com.portfolio.hireflow.auth.entity.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "google_calendar_connections")
public class GoogleCalendarConnection {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", unique = true)
  private AppUser user;

  @Column(name = "access_token_encrypted", nullable = false, length = 4000)
  private String accessTokenEncrypted;

  @Column(name = "refresh_token_encrypted", length = 4000)
  private String refreshTokenEncrypted;

  @Column(name = "access_token_expires_at", nullable = false)
  private Instant accessTokenExpiresAt;

  @Column(name = "granted_scope", length = 1000)
  private String grantedScope;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected GoogleCalendarConnection() {}

  public GoogleCalendarConnection(AppUser user) {
    this.user = user;
    this.createdAt = Instant.now();
    this.updatedAt = createdAt;
  }

  public void update(String access, String refresh, Instant expires, String scope) {
    accessTokenEncrypted = access;
    if (refresh != null && !refresh.isBlank()) refreshTokenEncrypted = refresh;
    accessTokenExpiresAt = expires;
    grantedScope = scope;
    updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public AppUser getUser() {
    return user;
  }

  public String getAccessTokenEncrypted() {
    return accessTokenEncrypted;
  }

  public String getRefreshTokenEncrypted() {
    return refreshTokenEncrypted;
  }

  public Instant getAccessTokenExpiresAt() {
    return accessTokenExpiresAt;
  }

  public String getGrantedScope() {
    return grantedScope;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
