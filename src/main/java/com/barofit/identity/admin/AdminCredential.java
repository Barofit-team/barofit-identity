package com.barofit.identity.admin;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "admin_credentials", schema = "identity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminCredential {
  @Id
  @Column(length = 64)
  private String username;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false, length = 100)
  private String passwordHash;

  @Column(nullable = false)
  private int failedAttempts;

  private Instant lockedUntil;

  @Column(nullable = false)
  private Instant updatedAt;

  public AdminCredential(String username, UUID userId, String hash, Instant now) {
    this.username = username;
    this.userId = userId;
    passwordHash = hash;
    updatedAt = now;
  }

  public boolean locked(Instant now) {
    return lockedUntil != null && now.isBefore(lockedUntil);
  }

  public void failed(Instant now) {
    if (lockedUntil != null && !now.isBefore(lockedUntil)) failedAttempts = 0;
    if (++failedAttempts >= 5) lockedUntil = now.plusSeconds(300);
    updatedAt = now;
  }

  public void succeeded(Instant now) {
    failedAttempts = 0;
    lockedUntil = null;
    updatedAt = now;
  }

  public void changePassword(String hash, Instant now) {
    passwordHash = hash;
    succeeded(now);
  }
}
