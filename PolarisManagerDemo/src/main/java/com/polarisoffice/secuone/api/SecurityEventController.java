// src/main/java/com/polarisoffice/secuone/api/SecurityEventController.java
package com.polarisoffice.secuone.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarisoffice.secuone.domain.SecurityEventEntity;
import com.polarisoffice.secuone.repository.SecurityEventRepository;
import com.polarisoffice.secuone.service.TenantResolverService;
import jakarta.persistence.criteria.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/events")
public class SecurityEventController {

  private static final Logger log = LoggerFactory.getLogger(SecurityEventController.class);

  private static final String T_MALWARE = "MALWARES_APP";
  private static final String T_ROOTING = "ROOTING_DETECTED";
  private static final String T_REMOTE  = "REMOTE_CONTROL_APP";

  public record EventReq(
      String packageName,
      String domain,
      String deviceId,
      String eventType,
      Map<String,Object> data
  ) {}
  public record EventRes(boolean ok) {}

  private final TenantResolverService resolver;
  private final SecurityEventRepository repo;
  private final ObjectMapper om;

  public SecurityEventController(
      TenantResolverService resolver,
      SecurityEventRepository repo,
      ObjectMapper om
  ) {
    this.resolver = resolver;
    this.repo = repo;
    this.om = om;
  }

  /* ===================== 1) 이벤트 리포트 ===================== */
  @PostMapping("/report")
  public ResponseEntity<?> report(@RequestBody EventReq req) {
    var resolved = resolver.resolve(nz(req.packageName()), nz(req.domain()));
    if (resolved.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("message","고객사 식별 실패(패키지/도메인 매핑 없음)"));
    }
    var t = resolved.get();

    var e = new SecurityEventEntity();
    e.setCustomerCode(t.customerCode());
    e.setDeviceId(nz(req.deviceId()));
    e.setEventType(nz(req.eventType()));
    e.setSourcePackage(nz(req.packageName()));
    e.setSourceDomain(nz(req.domain()));
    e.setCreatedAt(Instant.now());

    if (T_MALWARE.equalsIgnoreCase(nz(req.eventType()))) {
      // data에서 악성앱 정보 추출
      MalwareInfo mi = extractMalwareFromData(req.data());

      // 🔎 로그: 악성앱 패키지/유형 찍기 (INFO)
      String rawShort = mi.raw == null ? null : (mi.raw.length() > 300 ? mi.raw.substring(0,300) + "..." : mi.raw);
      log.info("[SEC] MALWARE event: customer={}, deviceId={}, pkg={}, type={}, sourcePkg={}, domain={}, raw={}",
          t.customerCode(),
          nz(req.deviceId()),
          nz(mi.pkg),
          nz(mi.type),
          nz(req.packageName()),
          nz(req.domain()),
          rawShort
      );

      // 저장 페이로드(JSON)
      Map<String,Object> payload = new LinkedHashMap<>();
      if (mi.pkg  != null) payload.put("malwarePackage", mi.pkg);
      if (mi.type != null) payload.put("malwareType",   mi.type);
      if (mi.raw  != null) payload.put("raw",           mi.raw);
      e.setPayloadJson(toJson(payload));
    } else {
      // 기타 타입은 데이터 전체 저장
      e.setPayloadJson(toJson(req.data()));
    }

    repo.save(e);
    return ResponseEntity.ok(new EventRes(true));
  }

  /* ===================== 2) 대시보드 조회 ===================== */
  @GetMapping("/security")
  public Map<String, Object> list(
      @RequestParam String customerCode,
      @RequestParam String from,
      @RequestParam String to,
      @RequestParam(defaultValue = "ALL") String type,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt,DESC") String sort
  ) {
    ZoneId zone = ZoneId.systemDefault();
    Instant start = LocalDate.parse(from).atStartOfDay(zone).toInstant();
    Instant end   = LocalDate.parse(to).plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant();

    Specification<SecurityEventEntity> base =
        Specification.<SecurityEventEntity>where(eq("customerCode", customerCode))
                     .and(betweenInstant("createdAt", start, end));

    Specification<SecurityEventEntity> spec = base;
    if (!"ALL".equalsIgnoreCase(type)) {
      spec = spec.and(eq("eventType", type));
    }

    Sort s = Sort.by("createdAt").descending();
    if (sort != null && !sort.isBlank()) {
      String[] parts = sort.split(",");
      String prop = parts[0].trim();
      boolean desc = parts.length < 2 || !"ASC".equalsIgnoreCase(parts[1]);
      s = desc ? Sort.by(prop).descending() : Sort.by(prop).ascending();
    }
    Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), s);

    Page<SecurityEventEntity> pg = repo.findAll(spec, pageable);

    long total   = repo.count(base);
    long malware = repo.count(base.and(eq("eventType", T_MALWARE)));
    long rooting = repo.count(base.and(eq("eventType", T_ROOTING)));
    long remote  = repo.count(base.and(eq("eventType", T_REMOTE)));

    List<Map<String, Object>> items = new ArrayList<>();
    for (SecurityEventEntity e : pg.getContent()) {
      Map<String,Object> m = new LinkedHashMap<>();
      m.put("id",            e.getId());
      m.put("createdAt",     e.getCreatedAt());
      m.put("type",          e.getEventType());
      m.put("deviceId",      e.getDeviceId());
      m.put("sourcePkg",     e.getSourcePackage());
      m.put("sourceDomain",  e.getSourceDomain());
      m.put("payload",       e.getPayloadJson());

      if (T_MALWARE.equalsIgnoreCase(nz(e.getEventType()))) {
        MalwareInfo mi = extractMalwareFromPayloadJson(e.getPayloadJson());
        if (mi.pkg  != null && !mi.pkg.isBlank())  m.put("malwarePackage", mi.pkg);
        if (mi.type != null && !mi.type.isBlank()) m.put("malwareType",   mi.type);
      }
      items.add(m);
    }

    Map<String,Object> kpi = new LinkedHashMap<>();
    kpi.put("total", total);
    kpi.put("malware", malware);
    kpi.put("rooting", rooting);
    kpi.put("remote", remote);

    Map<String,Object> pageMap = new LinkedHashMap<>();
    pageMap.put("page", pg.getNumber());
    pageMap.put("size", pg.getSize());
    pageMap.put("totalElements", pg.getTotalElements());
    pageMap.put("totalPages", pg.getTotalPages());

    Map<String,Object> result = new LinkedHashMap<>();
    result.put("kpi", kpi);
    result.put("items", items);
    result.put("page", pageMap);
    return result;
  }

  /* ===================== 유틸 ===================== */

  private static String nz(String s){ return (s == null || s.isBlank()) ? null : s; }

  private String toJson(Object o){
    if (o == null) return "{}";
    try { return om.writeValueAsString(o); }
    catch (Exception ignore) { return "{}"; }
  }

  private static <T> Specification<T> eq(String field, Object val) {
    return (root, q, cb) -> (val == null) ? null : cb.equal(root.get(field), val);
  }

  private static Specification<SecurityEventEntity> betweenInstant(String field, Instant from, Instant to) {
    return (root, q, cb) -> {
      if (from == null && to == null) return null;
      Expression<Instant> expr = root.get(field);
      if (from != null && to != null) return cb.between(expr, from, to);
      if (from != null) return cb.greaterThanOrEqualTo(expr, from);
      return cb.lessThanOrEqualTo(expr, to);
    };
  }

  /* ===== 악성앱 정보 추출 ===== */

  private static class MalwareInfo {
    String pkg;   // com.xxx.yyy
    String type;  // Android.TestVirus
    String raw;   // 원본 문자열(있으면)
  }

  private MalwareInfo extractMalwareFromData(Map<String,Object> data) {
    MalwareInfo r = new MalwareInfo();
    if (data == null) return r;

    r.pkg  = toStrOrNull(data.get("malwarePackage"));
    if (r.pkg == null) r.pkg = toStrOrNull(data.get("pkg"));
    if (r.pkg == null) r.pkg = toStrOrNull(data.get("packageName"));

    r.type = toStrOrNull(data.get("malwareType"));
    if (r.type == null) r.type = toStrOrNull(data.get("type"));

    String raw = toStrOrNull(data.get("payload"));
    r.raw = raw;
    if ((r.pkg == null || r.type == null) && raw != null) {
      MalwareInfo fromRaw = extractMalwareFromRawText(raw);
      if (r.pkg == null)  r.pkg  = fromRaw.pkg;
      if (r.type == null) r.type = fromRaw.type;
    }
    return r;
  }

  private MalwareInfo extractMalwareFromPayloadJson(String payloadJson) {
    MalwareInfo r = new MalwareInfo();
    if (payloadJson == null || payloadJson.isBlank()) return r;

    try {
      JsonNode n = om.readTree(payloadJson);
      if (n.isObject()) {
        if (n.hasNonNull("malwarePackage") || n.hasNonNull("pkg") || n.hasNonNull("packageName")) {
          r.pkg = textOrNull(n.get("malwarePackage"));
          if (r.pkg == null) r.pkg = textOrNull(n.get("pkg"));
          if (r.pkg == null) r.pkg = textOrNull(n.get("packageName"));
        }
        if (n.hasNonNull("malwareType") || n.hasNonNull("type")) {
          r.type = textOrNull(n.get("malwareType"));
          if (r.type == null) r.type = textOrNull(n.get("type"));
        }
        if (n.hasNonNull("raw")) r.raw = textOrNull(n.get("raw"));
        if (r.pkg != null || r.type != null) return r;
      }
    } catch (Exception ignore) { /* not JSON */ }

    return extractMalwareFromRawText(payloadJson);
  }

  private static MalwareInfo extractMalwareFromRawText(String text) {
    MalwareInfo r = new MalwareInfo();
    if (text == null) return r;

    Pattern P = Pattern.compile(
        "/([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)[^/]*?/base\\.apk(?:\\s*,\\s*([^\\r\\n,]+))?",
        Pattern.CASE_INSENSITIVE);
    Matcher m = P.matcher(text);
    if (m.find()) {
      r.pkg = trimOrNull(m.group(1));
      r.type = (m.groupCount() >= 2) ? trimOrNull(m.group(2)) : null;
      return r;
    }

    int idx = text.lastIndexOf(',');
    if (idx >= 0 && idx + 1 < text.length()) {
      String t = text.substring(idx + 1).trim();
      int nl = t.indexOf('\n');
      if (nl >= 0) t = t.substring(0, nl).trim();
      if (!t.isEmpty()) r.type = t;
    }

    Matcher m2 = Pattern.compile("\\b(com[\\w.]+)\\b").matcher(text);
    if (m2.find()) r.pkg = m2.group(1);
    return r;
  }

  private static String toStrOrNull(Object v) {
    if (v == null) return null;
    String s = String.valueOf(v).trim();
    return s.isEmpty() ? null : s;
  }
  private static String textOrNull(JsonNode n) {
    if (n == null || n.isNull()) return null;
    String s = n.asText(null);
    return (s == null || s.isBlank()) ? null : s;
  }
  private static String trimOrNull(String s) {
    if (s == null) return null;
    s = s.trim();
    return s.isEmpty() ? null : s;
  }
}
