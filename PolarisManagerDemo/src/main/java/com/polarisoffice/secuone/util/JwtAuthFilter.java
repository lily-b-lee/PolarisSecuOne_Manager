// src/main/java/com/polarisoffice/secuone/util/JwtAuthFilter.java
package com.polarisoffice.secuone.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.polarisoffice.secuone.security.JwtTokenService;
import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtTokenService tokenService;

  public JwtAuthFilter(JwtTokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    // 1) 우선순위: Authorization → 쿠키 → 쿼리 파라미터
    String token = extractBearer(request);
    if (token == null) token = extractFromCookies(request, "access_token", "admin_token", "customer_token", "user_token");
    if (token == null) token = request.getParameter("token");

    var ctx = org.springframework.security.core.context.SecurityContextHolder.getContext();
    if (token != null && !token.isBlank() && ctx.getAuthentication() == null) {
      Claims claims = null;
      try {
        // JwtTokenService.decode()는 0.12 방식으로 Claims 반환하도록 구현되어 있음
        claims = tokenService.decode(token);
      } catch (RuntimeException ex) {
        claims = null; // 토큰 파싱 실패 시 무시하고 다음 필터로
      }

      if (claims != null) {
        // subject(사용자 식별자)
        String subject = nonBlankOr(
            claims.getSubject(),
            asString(claims.get("principal"))
        );

        // role / authorities (문자열, 컬렉션, 배열 등 다양한 형식 대비)
        String role = extractRole(claims);
        if (role == null || role.isBlank()) role = "USER";
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        // customerCode / tenantCode
        String customerCode = nonBlankOr(
            asString(claims.get("customerCode")),
            asString(claims.get("tenantCode"))
        );
        if (customerCode == null) customerCode = "";

        // principal 맵 구성 (null 금지라서 맵에 빈 문자열로 넣음)
        Map<String, Object> principalMap = new HashMap<>();
        principalMap.put("subject", subject == null ? "" : subject);
        principalMap.put("role", role);
        principalMap.put("customerCode", customerCode);

        var authToken = new UsernamePasswordAuthenticationToken(
            principalMap,
            null,
            List.of(new SimpleGrantedAuthority(roleWithPrefix))
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        ctx.setAuthentication(authToken);
      }
    }

    filterChain.doFilter(request, response);
  }

  private static String extractRole(Claims claims) {
    Object r = claims.get("role");
    if (r == null) r = claims.get("authorities");
    if (r == null) return null;

    if (r instanceof String s) {
      return s.trim();
    }
    if (r instanceof Collection<?> col) {
      for (Object o : col) {
        String s = asString(o);
        if (s != null && !s.isBlank()) return s.trim();
      }
      return null;
    }
    if (r.getClass().isArray()) {
      Object[] arr = (Object[]) r;
      return (arr.length > 0) ? asString(arr[0]) : null;
    }
    return asString(r);
  }

  private static String asString(Object o) {
    return (o == null) ? null : String.valueOf(o);
  }

  private static String nonBlankOr(String a, String b) {
    if (a != null && !a.isBlank()) return a;
    if (b != null && !b.isBlank()) return b;
    return null;
  }

  private String extractBearer(HttpServletRequest req) {
    String auth = req.getHeader("Authorization");
    if (auth != null && auth.startsWith("Bearer ")) {
      return auth.substring(7).trim();
    }
    // X-Auth-Token 호환
    String x = req.getHeader("X-Auth-Token");
    return (x != null && !x.isBlank()) ? x.trim() : null;
  }

  private String extractFromCookies(HttpServletRequest req, String... names) {
    Cookie[] cookies = req.getCookies();
    if (cookies == null) return null;
    for (String n : names) {
      for (Cookie c : cookies) {
        if (n.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
          return c.getValue();
        }
      }
    }
    return null;
  }
}
