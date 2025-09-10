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

  /** 단일 체인: 정적/로그인/뷰 라우트 permitAll + CSP 헤더 */
  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable());
    http.httpBasic(b -> b.disable());     // 브라우저 Basic 팝업 방지
    http.formLogin(f -> f.disable());     // 폼 로그인 미사용(커스텀 API 사용)

    http.authorizeHttpRequests(auth -> auth
        // 공개 라우트: 정적/리소스/로그인 API/회원가입/비번찾기/이미지 프록시
        .requestMatchers(
            "/login", "/api/auth/login",
            "/signup", "/forgot-password",
            "/css/**", "/js/**", "/images/**", "/img/**", "/webjars/**",
            "/img-proxy" // 이미지 프록시 공개
        ).permitAll()

        // 뉴스레터 API 읽기 공개
        .requestMatchers("/secu-news/**", "/newsletters/**").permitAll()

        // ✅ 공지 API 읽기/쓰기 공개(필요 시 authenticated()로 변경)
        .requestMatchers(
            "/api/notices/**", "/notices/**",
            "/api/notice/**",  "/notice/**"
        ).permitAll()

        // 뷰 라우트: 서버는 공개, 접근제어는 프런트 JS 가드로
        .requestMatchers(
            "/", "/overview",
            "/manager/**",
            "/newsletter", "/polarletter", "/notice", "/track_report",
            "/customers/**"
        ).permitAll()

        // 그 외
        .anyRequest().permitAll()
    );

    // ✅ CSP: 외부 이미지 허용(data:, https:)
    http.headers(h -> h
        .contentSecurityPolicy(csp -> csp.policyDirectives(String.join(" ",
            "default-src 'self';",
            "img-src 'self' data: https:;",
            "script-src 'self';",
            "style-src 'self' 'unsafe-inline';",
            // 로컬 개발에서 http API 호출이 있다면 아래에 http:도 추가
            // "connect-src 'self' https: http:;",
            "connect-src 'self' https:;"
        )))
        .referrerPolicy(rp -> rp.policy(
            org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER
        ))
        .frameOptions(f -> f.sameOrigin())
    );

    return http.build();
  }
}
