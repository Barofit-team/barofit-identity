package com.barofit.identity.admin;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.barofit.identity.user.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class AdminMemberServiceTest {
  @Test
  void checksCurrentDatabaseRoleAndStatusRatherThanOnlyJwtRole() {
    var users = mock(UserRepository.class);
    var id = UUID.randomUUID();
    var service = new AdminMemberService(users);
    when(users.findById(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.requireAdmin(id)).isInstanceOf(AccessDeniedException.class);
    var user = mock(User.class);
    when(users.findById(id)).thenReturn(Optional.of(user));
    when(user.getRole()).thenReturn(UserRole.USER);
    assertThatThrownBy(() -> service.requireAdmin(id)).isInstanceOf(AccessDeniedException.class);
    when(user.getRole()).thenReturn(UserRole.ADMIN);
    when(user.getStatus()).thenReturn(UserStatus.WITHDRAWN);
    assertThatThrownBy(() -> service.requireAdmin(id)).isInstanceOf(AccessDeniedException.class);
    when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
    assertThat(service.requireAdmin(id).role()).isEqualTo(UserRole.ADMIN);
  }
}
