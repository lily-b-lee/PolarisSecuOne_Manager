// src/main/java/com/polarisoffice/secuone/config/MvcConfig.java
package com.polarisoffice.secuone.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    // 루트 → /overview 로 302 리다이렉트
    registry.addRedirectViewController("/", "/overview");
  }
}
