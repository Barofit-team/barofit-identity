package com.barofit.identity.admin;

import com.barofit.identity.user.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {
  private final UserRepository users;

  public record Member(
      UUID id,
      String nickname,
      UserRole role,
      UserStatus status,
      boolean onboarded,
      Instant createdAt,
      Instant lastLoginAt) {
    static Member from(User u) {
      return new Member(
          u.getId(),
          u.getNickname(),
          u.getRole(),
          u.getStatus(),
          u.isOnboarded(),
          u.getCreatedAt(),
          u.getLastLoginAt());
    }
  }

  public record Members(List<Member> items, long total, int page, int totalPages) {}

  public record Summary(long total, long active, long onboarded, long joinedToday) {}

  public Member requireAdmin(UUID id) {
    var user = users.findById(id).orElseThrow(() -> new AccessDeniedException("관리자 계정이 필요합니다."));
    if (user.getRole() != UserRole.ADMIN || user.getStatus() != UserStatus.ACTIVE)
      throw new AccessDeniedException("활성 관리자 계정이 필요합니다.");
    return Member.from(user);
  }

  public Members list(UUID admin, String query, int page, int size) {
    requireAdmin(admin);
    var result =
        users.findByNicknameContainingIgnoreCase(
            query.strip(),
            PageRequest.of(
                page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    return new Members(
        result.map(Member::from).getContent(),
        result.getTotalElements(),
        page,
        result.getTotalPages());
  }

  public Summary summary(UUID admin) {
    requireAdmin(admin);
    return new Summary(
        users.count(),
        users.countByStatus(UserStatus.ACTIVE),
        users.countByOnboardedTrueAndStatus(UserStatus.ACTIVE),
        users.countByCreatedAtGreaterThanEqual(
            LocalDate.now(ZoneId.of("Asia/Seoul"))
                .atStartOfDay(ZoneId.of("Asia/Seoul"))
                .toInstant()));
  }
}
