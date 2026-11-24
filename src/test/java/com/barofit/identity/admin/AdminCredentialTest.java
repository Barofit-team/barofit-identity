package com.barofit.identity.admin;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminCredentialTest {
  @Test
  void fifthFailureLocksForFiveMinutesAndSuccessfulLoginResetsAttempts() {
    var now = Instant.parse("2026-11-24T00:00:00Z");
    var c = new AdminCredential("operator", UUID.randomUUID(), "hash", now);
    for (int i = 0; i < 4; i++) c.failed(now);
    assertThat(c.locked(now)).isFalse();
    c.failed(now);
    assertThat(c.locked(now.plusSeconds(299))).isTrue();
    assertThat(c.locked(now.plusSeconds(300))).isFalse();
    c.failed(now.plusSeconds(301));
    assertThat(c.getFailedAttempts()).isEqualTo(1);
    c.succeeded(now.plusSeconds(302));
    assertThat(c.getFailedAttempts()).isZero();
    assertThat(c.getLockedUntil()).isNull();
  }

  @Test
  void bcryptPasswordPolicyUsesUtf8ByteLimit() {
    assertThat(AdminPasswordService.validPassword("very-secure-password")).isTrue();
    assertThat(AdminPasswordService.validPassword("short")).isFalse();
    assertThat(AdminPasswordService.validPassword("가".repeat(25))).isFalse();
    assertThat(AdminPasswordService.validPassword("가".repeat(24))).isTrue();
  }
}
