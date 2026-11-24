package com.barofit.identity.admin;

import com.barofit.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AdminErrorCode implements ErrorCode {
  ADMIN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "활성 관리자 계정만 접근할 수 있습니다."),
  ADMIN_LOGIN_INVALID(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호를 확인해 주세요. 반복 실패 시 5분 후 다시 시도해 주세요."),
  ADMIN_LOGIN_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "로그인 요청이 많습니다. 잠시 후 다시 시도해 주세요."),
  ADMIN_PASSWORD_INVALID(HttpStatus.BAD_REQUEST, "비밀번호는 12~64자 및 UTF-8 72바이트 이하로 입력하세요.");
  private final HttpStatus status;
  private final String message;

  AdminErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  public String code() {
    return name();
  }

  public String message() {
    return message;
  }

  public HttpStatus status() {
    return status;
  }
}
