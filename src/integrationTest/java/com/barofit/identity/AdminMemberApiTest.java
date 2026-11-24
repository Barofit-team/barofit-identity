package com.barofit.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.barofit.identity.user.*;
import com.barofit.testsupport.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IdentityIntegrationTest
@AutoConfigureMockMvc
@Transactional
class AdminMemberApiTest {
  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired JdbcTemplate jdbc;
  @Autowired jakarta.persistence.EntityManager em;

  String token(UUID id, String role) {
    return "Bearer "
        + BarofitTestTokens.accessToken(
            BarofitJwtKeyTestSupport.KEYS.keyPair(), id, Instant.now(), c -> c.claim("role", role));
  }

  @Test
  void requiresSignedAdminAndCurrentActiveAdminAccount() throws Exception {
    var u = users.saveAndFlush(User.register("관리자검색회원", null, Instant.now()));
    mvc.perform(get("/api/v1/admin/identity/members")).andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/v1/admin/identity/members").header("Authorization", token(u.getId(), "USER")))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/admin/identity/session")
                .header("Authorization", token(u.getId(), "ADMIN")))
        .andExpect(status().isForbidden());
    jdbc.update("UPDATE identity.users SET role='ADMIN' WHERE id=?", u.getId());
    em.clear();
    mvc.perform(
            get("/api/v1/admin/identity/session")
                .header("Authorization", token(u.getId(), "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"));
    mvc.perform(
            get("/api/v1/admin/identity/members")
                .param("query", "관리자검색회원")
                .header("Authorization", token(u.getId(), "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.items[0].nickname").value("관리자검색회원"));
    mvc.perform(
            get("/api/v1/admin/identity/summary")
                .header("Authorization", token(u.getId(), "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").isNumber());
    mvc.perform(
            get("/api/v1/admin/identity/members")
                .param("size", "101")
                .header("Authorization", token(u.getId(), "ADMIN")))
        .andExpect(status().isBadRequest());
    jdbc.update("UPDATE identity.users SET status='WITHDRAWN' WHERE id=?", u.getId());
    em.clear();
    mvc.perform(
            get("/api/v1/admin/identity/summary")
                .header("Authorization", token(u.getId(), "ADMIN")))
        .andExpect(status().isForbidden());
  }
}
