// src/main/java/com/polarisoffice/secuone/support/DomainCustomerResolver.java
package com.polarisoffice.secuone.support;

import com.polarisoffice.secuone.repository.CustomerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DomainCustomerResolver {

  private final CustomerRepository customers;

  // 예: app.example.com 형태에서 subdomain을 code로 쓸지 여부
  @Value("${tenant.subdomain.enabled:true}")
  private boolean subdomainEnabled;

  // 루트도메인(선택). 예: example.com
  @Value("${tenant.root-domain:}")
  private String rootDomain;

  public DomainCustomerResolver(CustomerRepository customers) {
    this.customers = customers;
  }

  public String resolveCustomerCode(HttpServletRequest req) {
    String host = header(req, "X-Forwarded-Host");
    if (host == null || host.isBlank()) {
      host = req.getServerName();
    }
    if (host == null) return null;

    host = host.toLowerCase();
    int p = host.indexOf(':');
    if (p >= 0) host = host.substring(0, p);

    // 1) DB에서 도메인 매핑 우선
    Optional<String> byDb = customers.findByDomainIgnoreCase(host).map(c -> c.getCode());
    if (byDb.isPresent()) return byDb.get();

    // 2) 서브도메인 패턴 허용 시: <code>.<rootDomain>
    if (subdomainEnabled && rootDomain != null && !rootDomain.isBlank()) {
      String rd = rootDomain.toLowerCase();
      if (host.endsWith("." + rd)) {
        String sub = host.substring(0, host.length() - rd.length() - 1);
        // 가장 왼쪽 레이블만 코드로 사용 (foo.bar.example.com -> foo)
        int dot = sub.indexOf('.');
        if (dot >= 0) sub = sub.substring(0, dot);
        if (!sub.isBlank()) return sub;
      }
    }

    return null;
  }

  private static String header(HttpServletRequest req, String name) {
    String v = req.getHeader(name);
    return (v == null || v.isBlank()) ? null : v;
  }
}
