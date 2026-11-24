package com.barofit.identity.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "barofit.admin.bootstrap.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {
  private final AdminPasswordService service;
  private final String username, password;

  public AdminBootstrap(
      AdminPasswordService service,
      @Value("${barofit.admin.bootstrap.username:}") String username,
      @Value("${barofit.admin.bootstrap.password:}") String password) {
    this.service = service;
    this.username = username;
    this.password = password;
  }

  public void run(ApplicationArguments args) {
    service.bootstrap(username, password);
  }
}
