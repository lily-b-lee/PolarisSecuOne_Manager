// src/main/java/.../util/JwtAuthFilter.java
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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtTokenService tokenService; // decode(String) -> Optional<PrincipalOrMap>

  public JwtAuthFilter(JwtTokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    // 1) 우선순위: Authorization 헤더 → 쿠키 → 쿼리파라미터
    String token = extractBearer(request);
    if (token == null) token = extractFromCookies(request, "access_token", "admin_token", "customer_token", "user_token");
    if (token == null) token = request.getParameter("token");

    if (token != null && !token.isBlank() &&
        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null) {

	tokenService.decode(token).ifPresent(principalObj -> {
	    String role = "USER";
	    String customerCode = null;

	    if (principalObj instanceof Map<?,?> map) {
	      Object r = map.get("role");
	      if (r == null) r = map.get("authorities");
	      if (r != null) role = String.valueOf(r);

	      Object cc = map.get("customerCode");
	      if (cc == null) cc = map.get("tenantCode");
	      if (cc != null) customerCode = String.valueOf(cc);
	    } else {
	      try {
	        var m = principalObj.getClass().getMethod("getRole");
	        Object r = m.invoke(principalObj);
	        if (r != null) role = String.valueOf(r);
	      } catch (Exception ignored) {}
	      try {
	        var m = principalObj.getClass().getMethod("getCustomerCode");
	        Object cc = m.invoke(principalObj);
	        if (cc != null) customerCode = String.valueOf(cc);
	      } catch (Exception ignored) {}
	    }

	    if (!role.startsWith("ROLE_")) role = "ROLE_" + role;

	    var authToken = new UsernamePasswordAuthenticationToken(
	        (principalObj instanceof Map<?,?>) ? principalObj : Map.of(
	            "principal", String.valueOf(principalObj),
	            "role", role.replace("ROLE_",""),
	            "customerCode", customerCode == null ? "" : customerCode
	        ),
	        null,
	        List.of(new SimpleGrantedAuthority(role))
	    );
	    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
	    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authToken);
	  });
    }

    filterChain.doFilter(request, response);
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
