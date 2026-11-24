package com.barofit.identity.admin;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AdminCredentialRepository extends JpaRepository<AdminCredential, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from AdminCredential c where c.username=:username")
  Optional<AdminCredential> findForLogin(String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from AdminCredential c where c.userId=:userId")
  Optional<AdminCredential> findForUser(UUID userId);
}
