package com.polarisoffice.secuone.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** 단일 체인: 정적/로그인/뷰 라우트 permitAll (프론트 JS 가드 사용) */
  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable());
    http.httpBasic(b -> b.disable());   // 브라우저 Basic 팝업 방지
    http.formLogin(f -> f.disable());   // 폼 로그인 미사용(커스텀 API 사용)

    http.authorizeHttpRequests(auth -> auth
        // 공개 라우트: 정적/리소스/로그인 API/회원가입/비번찾기
        .requestMatchers(
            "/login", "/api/auth/login",
            "/signup", "/forgot-password",
            "/css/**", "/js/**", "/images/**", "/img/**", "/webjars/**"
        ).permitAll()

        // 뷰 라우트: 서버는 공개, 접근제어는 프런트 JS 가드로
        .requestMatchers(
            "/", "/overview",
            "/manager/**",
            "/newsletter", "/polarletter", "/notice", "/track_report",
            "/customers/**"
        ).permitAll()

        // 나머지도 일단 공개 (원하면 authenticated()로 변경)
        .anyRequest().permitAll()
    );

    return http.build();
  }
}
