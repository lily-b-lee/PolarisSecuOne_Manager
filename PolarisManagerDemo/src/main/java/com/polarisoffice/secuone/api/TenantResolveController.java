// src/main/java/com/polarisoffice/secuone/api/TenantResolveController.java
package com.polarisoffice.secuone.api;

import com.polarisoffice.secuone.service.TenantResolverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tenant")
public class TenantResolveController {

  public record ResolveReq(String packageName, String domain) {}
  public record ResolveRes(String customerCode, String customerName,
                           String matchedBy, String matchedKey) {}

  private final TenantResolverService resolver;

  public TenantResolveController(TenantResolverService resolver) {
    this.resolver = resolver;
  }

  @PostMapping("/resolve")
  public ResponseEntity<?> resolve(@RequestBody ResolveReq req) {
    var opt = resolver.resolve(req.packageName(), req.domain());
    if (opt.isEmpty()) {
      return ResponseEntity.status(404).body(Map.of("message", "매핑된 고객사를 찾을 수 없습니다."));
    }
    var r = opt.get();
    return ResponseEntity.ok(new ResolveRes(r.customerCode(), r.customerName(),
        r.matchedBy(), r.matchedKey()));
  }
}
