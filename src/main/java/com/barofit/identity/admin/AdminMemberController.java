package com.barofit.identity.admin;

import jakarta.validation.constraints.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/identity")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMemberController {
  private final AdminMemberService service;
  private final AdminPasswordService passwords;

  record PasswordEdit(
      @jakarta.validation.constraints.NotBlank @Size(max = 64) String currentPassword,
      @jakarta.validation.constraints.NotBlank @Size(min = 12, max = 64) String newPassword) {}

  @PutMapping("/password")
  public java.util.Map<String, Boolean> password(
      @AuthenticationPrincipal Jwt jwt, @jakarta.validation.Valid @RequestBody PasswordEdit edit) {
    service.requireAdmin(UUID.fromString(jwt.getSubject()));
    var error =
        passwords.changePassword(
            UUID.fromString(jwt.getSubject()), edit.currentPassword(), edit.newPassword());
    if (error != null) throw new com.barofit.common.error.BusinessException(error);
    return java.util.Map.of("ok", true);
  }

  @GetMapping("/session")
  public AdminMemberService.Member session(@AuthenticationPrincipal Jwt jwt) {
    return service.requireAdmin(UUID.fromString(jwt.getSubject()));
  }

  @GetMapping("/summary")
  public AdminMemberService.Summary summary(@AuthenticationPrincipal Jwt jwt) {
    return service.summary(UUID.fromString(jwt.getSubject()));
  }

  @GetMapping("/members")
  public AdminMemberService.Members members(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(defaultValue = "") @Size(max = 100) String query,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.list(UUID.fromString(jwt.getSubject()), query, page, size);
  }
}
