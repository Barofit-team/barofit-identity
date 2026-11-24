package com.barofit.identity;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.barofit.identity.admin.*;
import com.barofit.identity.user.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.*;

@IdentityIntegrationTest
@AutoConfigureMockMvc
class AdminPasswordApiTest {
  @Autowired MockMvc mvc;
  @Autowired AdminPasswordService passwords;
  @Autowired AdminCredentialRepository credentials;
  @Autowired UserRepository users;
  @Autowired JdbcTemplate jdbc;
  static final String USERNAME = "test-admin-operator";
  static final String PASSWORD = "Local-test-only-password-42!";

  @BeforeEach
  void create() {
    passwords.bootstrap(USERNAME, PASSWORD);
  }

  @AfterEach
  void cleanup() {
    credentials
        .findById(USERNAME)
        .ifPresent(c -> {
          jdbc.update("DELETE FROM identity.refresh_tokens WHERE user_id=?", c.getUserId());
          credentials.deleteById(USERNAME);
          users.deleteById(c.getUserId());
        });
  }

  ResultActions login(String username, String password) throws Exception {
    return mvc.perform(
        post("/api/v1/auth/admin-login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
  }

  @Test
  void passwordLoginNeedsNoSocialAccountAndStoresOnlyBcryptHash() throws Exception {
    var c = credentials.findById(USERNAME).orElseThrow();
    assertThat(c.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
    assertThat(new BCryptPasswordEncoder().matches(PASSWORD, c.getPasswordHash())).isTrue();
    var result =
        login(USERNAME, PASSWORD)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.role").value("ADMIN"))
            .andReturn();
    String access = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    mvc.perform(get("/api/v1/admin/identity/session").header("Authorization", "Bearer " + access))
        .andExpect(status().isOk());
    passwords.bootstrap(USERNAME, "A-different-password-42!");
    login(USERNAME, PASSWORD).andExpect(status().isOk());
  }

  @Test
  void failuresPersistAcrossUnauthorizedResponsesAndLockOutCorrectPassword() throws Exception {
    login("unknown-admin", "wrong-password").andExpect(status().isUnauthorized());
    for (int i = 0; i < 5; i++)
      login(USERNAME, "wrong-password").andExpect(status().isUnauthorized());
    assertThat(credentials.findById(USERNAME).orElseThrow().getFailedAttempts()).isEqualTo(5);
    login(USERNAME, PASSWORD).andExpect(status().isUnauthorized());
    jdbc.update(
        "UPDATE identity.admin_credentials SET locked_until=now()-interval '1 second' WHERE username=?",
        USERNAME);
    login(USERNAME, PASSWORD).andExpect(status().isOk());
  }

  @Test
  void changingPasswordRevokesRefreshTokensAndRequiresNewPassword() throws Exception {
    var result =
        login(USERNAME, PASSWORD)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String access = JsonPath.read(result, "$.accessToken"),
        refresh = JsonPath.read(result, "$.refreshToken");
    mvc.perform(
            put("/api/v1/admin/identity/password")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"currentPassword\":\""
                        + PASSWORD
                        + "\",\"newPassword\":\"New-test-password-42!\"}"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refresh + "\"}"))
        .andExpect(status().isUnauthorized());
    login(USERNAME, PASSWORD).andExpect(status().isUnauthorized());
    login(USERNAME, "New-test-password-42!").andExpect(status().isOk());
  }
}
