package com.barofit.identity.admin;

import com.barofit.identity.auth.LoginResult;
import com.barofit.identity.token.*;
import com.barofit.identity.user.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPasswordService {
  private final AdminCredentialRepository credentials;
  private final UserRepository users;
  private final SessionTokens tokens;
  private final RefreshTokenRepository refreshTokens;
  private final Clock clock;
  private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(12);
  private final String dummyHash = passwords.encode(UUID.randomUUID().toString());
  private long windowStart;
  private int attempts;

  public record Outcome(LoginResult login, AdminErrorCode error) {}

  public AdminPasswordService(
      AdminCredentialRepository credentials,
      UserRepository users,
      SessionTokens tokens,
      RefreshTokenRepository refreshTokens,
      Clock clock) {
    this.credentials = credentials;
    this.users = users;
    this.tokens = tokens;
    this.refreshTokens = refreshTokens;
    this.clock = clock;
  }

  private synchronized boolean allowAttempt() {
    long now = clock.millis();
    if (now - windowStart >= 60000) {
      windowStart = now;
      attempts = 0;
    }
    return ++attempts <= 30;
  }

  public static boolean validPassword(String value) {
    return value != null
        && value.length() >= 12
        && value.length() <= 64
        && value.getBytes(StandardCharsets.UTF_8).length <= 72;
  }

  /** 실패 카운터도 커밋해야 하므로 예외 대신 결과를 반환한다. */
  @Transactional
  public Outcome login(String username, String password) {
    if (!allowAttempt()) return new Outcome(null, AdminErrorCode.ADMIN_LOGIN_RATE_LIMITED);
    if (password.getBytes(StandardCharsets.UTF_8).length > 72)
      return new Outcome(null, AdminErrorCode.ADMIN_LOGIN_INVALID);
    var credential = credentials.findForLogin(username.toLowerCase(Locale.ROOT));
    if (credential.isEmpty()) {
      passwords.matches(password, dummyHash);
      return new Outcome(null, AdminErrorCode.ADMIN_LOGIN_INVALID);
    }
    var c = credential.get();
    var now = clock.instant();
    if (c.locked(now)) return new Outcome(null, AdminErrorCode.ADMIN_LOGIN_INVALID);
    if (password.getBytes(StandardCharsets.UTF_8).length > 72
        || !passwords.matches(password, c.getPasswordHash())) {
      c.failed(now);
      return new Outcome(null, AdminErrorCode.ADMIN_LOGIN_INVALID);
    }
    var user = users.findById(c.getUserId()).orElseThrow();
    if (user.getRole() != UserRole.ADMIN || !user.isActive())
      return new Outcome(null, AdminErrorCode.ADMIN_LOGIN_INVALID);
    c.succeeded(now);
    user.recordLogin(now);
    return new Outcome(new LoginResult(tokens.startSession(user, now), user), null);
  }

  @Transactional
  public void bootstrap(String username, String password) {
    if (!username.matches("[a-z0-9][a-z0-9._-]{2,63}") || !validPassword(password))
      throw new IllegalArgumentException(
          "관리자 아이디는 소문자·숫자·._- 3~64자, 비밀번호는 12~64자 및 UTF-8 72바이트 이하가 필요합니다.");
    if (credentials.existsById(username)) return;
    if (credentials.count() > 0) throw new IllegalStateException("최초 관리자 생성은 한 번만 가능합니다.");
    var now = clock.instant();
    var user = users.saveAndFlush(User.registerAdmin("바로핏 관리자", now));
    credentials.save(new AdminCredential(username, user.getId(), passwords.encode(password), now));
  }

  @Transactional
  public AdminErrorCode changePassword(UUID id, String current, String next) {
    if (!validPassword(next)) return AdminErrorCode.ADMIN_PASSWORD_INVALID;
    var c = credentials.findForUser(id).orElse(null);
    var user = users.findById(id).orElse(null);
    var now = clock.instant();
    if (c == null || user == null || user.getRole() != UserRole.ADMIN || !user.isActive())
      return AdminErrorCode.ADMIN_LOGIN_INVALID;
    if (c.locked(now)) return AdminErrorCode.ADMIN_LOGIN_INVALID;
    if (current.getBytes(StandardCharsets.UTF_8).length > 72
        || !passwords.matches(current, c.getPasswordHash())) {
      c.failed(now);
      return AdminErrorCode.ADMIN_LOGIN_INVALID;
    }
    c.changePassword(passwords.encode(next), now);
    refreshTokens.revokeAllByUserId(id, now);
    return null;
  }
}
