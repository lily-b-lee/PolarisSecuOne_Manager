// src/main/java/com/polarisoffice/secuone/service/TenantResolverService.java
package com.polarisoffice.secuone.service;

import com.polarisoffice.secuone.domain.CustomerBindingEntity;
import com.polarisoffice.secuone.repository.CustomerBindingRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TenantResolverService {

  public record Resolved(String customerCode, String customerName,
                         String matchedBy, String matchedKey) {}

  private final CustomerBindingRepository bindings;

  public TenantResolverService(CustomerBindingRepository bindings) {
    this.bindings = bindings;
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = "tenantResolve", key = "T(java.util.Objects).toString(#pkg)+'|'+T(java.util.Objects).toString(#domain)")
  public Optional<Resolved> resolve(String pkg, String domainHost) {
    // 1) 앱 패키지로 우선 시도
    if (pkg != null && !pkg.isBlank()) {
      var opt = bindings.findFirstByTypeAndKeyIgnoreCaseAndIsActiveTrueOrderByPriorityDesc(
          CustomerBindingEntity.BindingType.APP, pkg.trim().toLowerCase());
      if (opt.isPresent()) {
        var b = opt.get();
        var c = b.getCustomer();
        return Optional.of(new Resolved(c.getCode(), c.getName(), "package", pkg));
      }
    }
    // 2) 웹 도메인(호스트)로 시도: 정확히 ==, 또는 like(%.example.com)
    if (domainHost != null && !domainHost.isBlank()) {
      String host = domainHost.trim().toLowerCase();
      var list = bindings.findWebMatches(CustomerBindingEntity.BindingType.WEB, host);
      if (!list.isEmpty()) {
        var b = list.get(0); // priority desc 정렬
        var c = b.getCustomer();
        return Optional.of(new Resolved(c.getCode(), c.getName(), "domain", b.getKey()));
      }
    }
    return Optional.empty();
  }
}
