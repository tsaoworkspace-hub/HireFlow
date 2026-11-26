package com.portfolio.hireflow.integration.calendar.service;

import com.portfolio.hireflow.auth.entity.AppUser;
import com.portfolio.hireflow.integration.calendar.entity.GoogleCalendarConnection;
import com.portfolio.hireflow.integration.calendar.repository.GoogleCalendarConnectionRepository;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.util.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@ConditionalOnProperty(name = "app.google-calendar.enabled", havingValue = "true")
public class GoogleCalendarOAuthService {
  private final GoogleCalendarConnectionRepository repo;
  private final GoogleCalendarTokenCipher cipher;
  private final RestClient client = RestClient.create();
  private final String clientId, clientSecret, redirectUri;

  public GoogleCalendarOAuthService(
      GoogleCalendarConnectionRepository repo,
      GoogleCalendarTokenCipher cipher,
      @Value("${app.google-calendar.client-id}") String clientId,
      @Value("${app.google-calendar.client-secret}") String secret,
      @Value("${app.google-calendar.redirect-uri}") String redirect) {
    this.repo = repo;
    this.cipher = cipher;
    this.clientId = clientId;
    this.clientSecret = secret;
    this.redirectUri = redirect;
  }

  public String authorizationUrl(String state) {
    return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
        .queryParam("client_id", clientId)
        .queryParam("redirect_uri", redirectUri)
        .queryParam("response_type", "code")
        .queryParam("scope", "https://www.googleapis.com/auth/calendar.events")
        .queryParam("access_type", "offline")
        .queryParam("include_granted_scopes", "true")
        .queryParam("prompt", "consent")
        .queryParam("state", state)
        .build()
        .encode()
        .toUriString();
  }

  @Transactional
  public void exchange(AppUser user, String code) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("code", code);
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    form.add("redirect_uri", redirectUri);
    form.add("grant_type", "authorization_code");
    update(user, request(form), true);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String validAccessToken(Long userId) {
    GoogleCalendarConnection c =
        repo.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("尚未連結 Google Calendar"));
    if (c.getAccessTokenExpiresAt().isAfter(Instant.now().plusSeconds(30)))
      return cipher.decrypt(c.getAccessTokenEncrypted());
    String refresh = c.getRefreshTokenEncrypted();
    if (refresh == null) throw new IllegalStateException("缺少 Google Refresh Token，請重新連結");
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    form.add("refresh_token", cipher.decrypt(refresh));
    form.add("grant_type", "refresh_token");
    Map<String, Object> token = request(form);
    update(c.getUser(), token, false);
    return token.get("access_token").toString();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> request(MultiValueMap<String, String> form) {
    Map<String, Object> r =
        client
            .post()
            .uri("https://oauth2.googleapis.com/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);
    if (r == null || r.get("access_token") == null)
      throw new IllegalStateException("Google 未回傳 Access Token");
    return r;
  }

  private void update(AppUser user, Map<String, Object> token, boolean requireRefresh) {
    GoogleCalendarConnection c =
        repo.findByUserId(user.getId()).orElseGet(() -> new GoogleCalendarConnection(user));
    String refresh =
        token.get("refresh_token") == null
            ? null
            : cipher.encrypt(token.get("refresh_token").toString());
    if (requireRefresh && refresh == null && c.getRefreshTokenEncrypted() == null)
      throw new IllegalStateException("Google 未回傳 Refresh Token");
    long seconds = token.get("expires_in") instanceof Number n ? n.longValue() : 3600;
    c.update(
        cipher.encrypt(token.get("access_token").toString()),
        refresh,
        Instant.now().plusSeconds(Math.max(60, seconds - 60)),
        token.get("scope") == null ? null : token.get("scope").toString());
    repo.save(c);
  }

  @Transactional
  public void disconnect(Long userId) {
    repo.deleteByUserId(userId);
  }
}
