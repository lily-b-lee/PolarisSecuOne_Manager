package com.polarisoffice.secuone.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class DomainCustomerResolver {

  /** 외부에서 명시 코드가 있으면 그걸 사용하고, 없으면 Host 헤더에서 유추 */
  public String resolveCustomerCode(HttpServletRequest req, String explicit) {
    if (hasText(explicit)) return explicit;

    // 1) 프록시/커스텀 헤더 우선
    String host = headerFirst(req.getHeader("X-Customer-Domain"));
    if (!hasText(host)) host = headerFirst(req.getHeader("X-Forwarded-Host"));
    if (!hasText(host)) host = req.getServerName();

    if (hasText(host)) {
      host = normalizeHost(host);
      String code = guessCodeFromHost(host);
      if (hasText(code)) return code;
    }
    // 기본값
    return "mg";
  }

  // ───────── helpers ─────────
  private static boolean hasText(String s){ return s != null && !s.isBlank(); }

  /** 콤마로 연결된 헤더 값(프록시 체인)에서 첫 값만 사용 */
  private static String headerFirst(String headerVal) {
    if (!hasText(headerVal)) return null;
    int idx = headerVal.indexOf(',');
    return (idx > -1) ? headerVal.substring(0, idx).trim() : headerVal.trim();
  }

  /** 호스트 정규화: 소문자, 포트 제거, 선행 www. 제거 */
  private static String normalizeHost(String host) {
    String h = host.toLowerCase(Locale.ROOT).trim();
    h = h.replaceFirst(":\\d+$", "");       // :8080 제거
    if (h.startsWith("www.")) h = h.substring(4);
    return h;
  }

  /** 호스트에서 1레벨 서브도메인을 고객사 코드로 가정 */
  private static String guessCodeFromHost(String host) {
    // 예: acme.console.example.com -> "acme"
    String[] parts = host.split("\\.");
    if (parts.length == 0) return null;
    String first = parts[0];
    // 공용/관리 서브도메인은 제외
    Set<String> ignore = Set.of("www", "app", "manager", "console", "admin");
    if (ignore.contains(first)) return null;
    // 코드 형식(필요하면 규칙 조정)
    if (first.matches("[a-z0-9_-]{2,32}")) return first;
    return null;
  }
}
