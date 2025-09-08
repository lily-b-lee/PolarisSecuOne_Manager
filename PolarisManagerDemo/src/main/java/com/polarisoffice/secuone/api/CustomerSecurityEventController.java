// src/main/java/com/polarisoffice/secuone/api/CustomerSecurityEventController.java
package com.polarisoffice.secuone.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarisoffice.secuone.domain.SecurityEventEntity;
import com.polarisoffice.secuone.repository.CustomerRepository;
import com.polarisoffice.secuone.repository.SecurityEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.time.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/events/report")
public class CustomerSecurityEventController {

  private static final String T_MALWARE = "MALWARES_APP";
  private static final String T_ROOTING = "ROOTING_DETECTED";
  private static final String T_REMOTE  = "REMOTE_CONTROL_APP";

  private final SecurityEventRepository repo;
  private final CustomerRepository customerRepo;
  private final ObjectMapper om;
  private static final Logger log = LoggerFactory.getLogger(CustomerSecurityEventController.class);

  
  public CustomerSecurityEventController(SecurityEventRepository repo,
                                         CustomerRepository customerRepo,
                                         ObjectMapper om) {
    this.repo = repo;
    this.customerRepo = customerRepo;
    this.om = om;
  }

  /* -------------------------------------------------------
   * 유틸
   * ----------------------------------------------------- */
  private static boolean hasText(String s){ return s != null && !s.isBlank(); }

  private static String firstNonBlank(String... v){
    if (v == null) return null;
    for (String s : v) if (hasText(s)) return s;
    return null;
  }

  private static LocalDate tryParseLocalDate(String s, LocalDate def){
    try { return hasText(s) ? LocalDate.parse(s) : def; }
    catch (Exception ignore){ return def; }
  }

  /** principal/details 등 임의 객체에서 customerCode 유추 */
  private String tryFromObject(Object src) {
    if (src == null) return null;

    // 1) Map 형태
    if (src instanceof Map<?,?> map) {
      for (String k : List.of("customerCode","tenantCode","companyCode","code","cust","cc")) {
        Object v = map.get(k);
        if (v != null && hasText(String.valueOf(v))) return String.valueOf(v);
      }
    }

    // 2) 게터/필드
    for (String getter : List.of(
        "getCustomerCode","customerCode",
        "getTenantCode","tenantCode",
        "getCompanyCode","companyCode",
        "getCode","code")) {
      try {
        Method m = src.getClass().getMethod(getter);
        Object v = m.invoke(src);
        if (v != null && hasText(String.valueOf(v))) return String.valueOf(v);
      } catch (NoSuchMethodException ignored) {
      } catch (Exception ignored) { }
    }

    // 3) 문자열 패턴
    String s = String.valueOf(src);
    Matcher m = Pattern.compile("(?:customerCode|tenantCode|companyCode|code)=([A-Za-z0-9_-]+)").matcher(s);
    if (m.find()) return m.group(1);

    return null;
  }

  /** Authentication에서 클레임 꺼내기(리플렉션 기반, 라이브러리 무관) */
  @SuppressWarnings("unchecked")
  private Map<String, Object> claimsFromAuth(Authentication auth) {
    if (auth == null) return Collections.emptyMap();

    // 1) getTokenAttributes()
    try {
      var m = auth.getClass().getMethod("getTokenAttributes");
      Object v = m.invoke(auth);
      if (v instanceof Map<?,?> map) return (Map<String,Object>) map;
    } catch (NoSuchMethodException ignored) {
    } catch (Exception ignored) { }

    // 2) getToken() -> Jwt#getClaims()
    try {
      var mTok = auth.getClass().getMethod("getToken");
      Object jwt = mTok.invoke(auth);
      if (jwt != null) {
        var mClaims = jwt.getClass().getMethod("getClaims");
        Object c = mClaims.invoke(jwt);
        if (c instanceof Map<?,?> map) return (Map<String,Object>) map;
      }
    } catch (NoSuchMethodException ignored) {
    } catch (Exception ignored) { }

    // 3) principal이 Map
    Object p = auth.getPrincipal();
    if (p instanceof Map<?,?> map) return (Map<String,Object>) map;

    return Collections.emptyMap();
  }

  /** 가능한 모든 경로로 customerCode를 찾아본다. 실패시 도메인→코드 폴백 */
//교체: currentCustomerCode()
private String currentCustomerCode(HttpServletRequest req) {
 // 0) 쿼리 파라미터 우선 (?customerCode=mg, ?cc=mg)
 String byParam = Optional.ofNullable(req.getParameter("customerCode"))
     .orElse(req.getParameter("cc"));
 if (hasText(byParam)) {
   log.debug("[cust] resolved by query param: {}", byParam);
   return byParam;
 }

 // 1) 프록시/게이트웨이 헤더
 String byHeader = req.getHeader("X-Customer-Code");
 if (hasText(byHeader)) {
   log.debug("[cust] resolved by header X-Customer-Code: {}", byHeader);
   return byHeader;
 }

 // 2) SecurityContext (JWT/OAuth 토큰 클레임)
 var auth = SecurityContextHolder.getContext().getAuthentication();
 if (auth != null) {
   Map<String,Object> claims = claimsFromAuth(auth);
   for (String k : List.of("customerCode","tenantCode","companyCode","code","cust","cc")) {
     Object v = claims.get(k);
     if (v != null && hasText(String.valueOf(v))) {
       log.debug("[cust] resolved from JWT claims ({}): {}", k, v);
       return String.valueOf(v);
     }
   }
   String fromPrincipal = tryFromObject(auth.getPrincipal());
   if (hasText(fromPrincipal)) {
     log.debug("[cust] resolved from principal: {}", fromPrincipal);
     return fromPrincipal;
   }
   String fromDetails = tryFromObject(auth.getDetails());
   if (hasText(fromDetails)) {
     log.debug("[cust] resolved from details: {}", fromDetails);
     return fromDetails;
   }
 }

 // 3) 폴백: 도메인 → 고객사코드
 String domain = Optional.ofNullable(req.getHeader("X-Customer-Domain"))
     .orElseGet(() -> Optional.ofNullable(req.getHeader("X-Forwarded-Host"))
     .orElse(req.getServerName()));
 if (hasText(domain)) {
   domain = domain.replaceFirst(":\\d+$", ""); // 포트 제거
   String code = customerRepo.findByDomainIgnoreCase(domain)
       .map(c -> c.getCode())
       .orElse(null);
   log.debug("[cust] resolved by domain fallback {} -> {}", domain, code);
   return code;
 }

 log.warn("[cust] cannot resolve customerCode");
 return null;
}

  /* -------------------------------------------------------
   * API
   * ----------------------------------------------------- */

  /** 헬스체크 */
  @GetMapping("/_ping")
  public Map<String,Object> ping() {
    Map<String,Object> m = new LinkedHashMap<>();
    m.put("ok", true);
    m.put("ts", Instant.now().toString());
    return m;
  }

  /** 일별 집계 + 악성앱 Top (본인 고객사만) */
  @GetMapping("/daily")
  public ResponseEntity<?> daily(
      HttpServletRequest req,
      @RequestHeader(value = "X-Customer-Code", required = false) String ccHeader, // 선택 헤더
      @RequestParam(value="customerCode", required = false) String ccParam,        // 선택 쿼리
      @RequestParam(required = false) String from,     // YYYY-MM-DD
      @RequestParam(required = false) String to,       // YYYY-MM-DD
      @RequestParam(defaultValue = "Asia/Seoul") String tz
  ) {
    String customerCode = firstNonBlank(ccHeader, ccParam, currentCustomerCode(req));
    System.out.println("[/api/events/report/daily] X-CC=" + ccHeader
        + " Q-CC=" + ccParam
        + " RESOLVED=" + customerCode);

    if (!hasText(customerCode)) {
      Map<String,Object> err = new LinkedHashMap<>();
      err.put("ok", false);
      err.put("message", "고객사 식별 실패 (X-Customer-Code 헤더 또는 로그인 토큰의 customerCode 필요)");
      return ResponseEntity.status(403).body(err);
    }

    ZoneId zone = ZoneId.of(hasText(tz) ? tz : "Asia/Seoul");

    // 기간 파싱 (기본: 최근 7일)
    LocalDate toDate   = tryParseLocalDate(to,   LocalDate.now(zone));
    LocalDate fromDate = tryParseLocalDate(from, toDate.minusDays(6));

    Instant start = fromDate.atStartOfDay(zone).toInstant();
    Instant end   = toDate.plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant();

    // 1) 일별 x 타입 카운트
    List<SecurityEventRepository.DailyCountRow> rows =
        repo.countDailyByType(customerCode, start, end);

    // 2) 날짜 스켈레톤
    LinkedHashMap<LocalDate, Map<String, Long>> series = new LinkedHashMap<>();
    for (LocalDate d = fromDate; !d.isAfter(toDate); d = d.plusDays(1)) {
      Map<String, Long> day = new HashMap<>();
      day.put(T_MALWARE, 0L);
      day.put(T_ROOTING, 0L);
      day.put(T_REMOTE,  0L);
      series.put(d, day);
    }
    for (SecurityEventRepository.DailyCountRow r : rows) {
      LocalDate d = r.getDay();
      if (series.containsKey(d)) {
        series.get(d).put(r.getEventType(), r.getCnt());
      }
    }

    // 3) 응답 시리즈 + 합계
    long totalMal = 0, totalRoot = 0, totalRem = 0, total = 0;
    List<Map<String,Object>> outSeries = new ArrayList<>();
    for (Map.Entry<LocalDate, Map<String, Long>> e : series.entrySet()) {
      long m = e.getValue().getOrDefault(T_MALWARE, 0L);
      long r = e.getValue().getOrDefault(T_ROOTING, 0L);
      long rc= e.getValue().getOrDefault(T_REMOTE,  0L);
      long t = m + r + rc;
      totalMal += m; totalRoot += r; totalRem += rc; total += t;

      Map<String,Object> row = new LinkedHashMap<>();
      row.put("date",    e.getKey().toString());
      row.put("malware", m);
      row.put("rooting", r);
      row.put("remote",  rc);
      row.put("total",   t);
      outSeries.add(row);
    }

    // 4) 악성앱 Top N (유형/패키지)
    List<SecurityEventEntity> malEvents =
        repo.findByCustomerCodeAndEventTypeAndCreatedAtBetween(customerCode, T_MALWARE, start, end);

    Map<String, Long> typeCount = new HashMap<>();
    Map<String, Long> pkgCount  = new HashMap<>();
    for (SecurityEventEntity e : malEvents) {
      String payload = Optional.ofNullable(e.getPayloadJson()).orElse("");
      String type = extractMalwareType(payload);
      String pkg  = extractMalwarePackage(payload);
      if (!hasText(type)) type = "-";
      if (!hasText(pkg))  pkg  = "-";
      typeCount.merge(type, 1L, Long::sum);
      pkgCount.merge(pkg, 1L, Long::sum);
    }

    List<Map<String,Object>> topType = typeCount.entrySet().stream()
        .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
        .limit(10)
        .map(e -> {
          Map<String,Object> m = new LinkedHashMap<>();
          m.put("type",  e.getKey());
          m.put("count", e.getValue());
          return m;
        })
        .collect(Collectors.toList());

    List<Map<String,Object>> topPkg = pkgCount.entrySet().stream()
        .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
        .limit(10)
        .map(e -> {
          Map<String,Object> m = new LinkedHashMap<>();
          m.put("package", e.getKey());
          m.put("count",   e.getValue());
          return m;
        })
        .collect(Collectors.toList());

    // 5) 응답(JSON)
    Map<String,Object> res = new LinkedHashMap<>();
    Map<String,Object> range = new LinkedHashMap<>();
    Map<String,Object> totals = new LinkedHashMap<>();

    res.put("ok", true);

    range.put("from", fromDate.toString());
    range.put("to",   toDate.toString());
    range.put("tz",   zone.getId());
    res.put("range", range);

    res.put("series", outSeries);

    totals.put("malware", totalMal);
    totals.put("rooting", totalRoot);
    totals.put("remote",  totalRem);
    totals.put("total",   total);
    res.put("totals", totals);

    res.put("malwareTopTypes", topType);
    res.put("malwareTopPackages", topPkg);

    return ResponseEntity.ok(res);
  }

  /* -------------------------------------------------------
   * Payload 파싱
   * ----------------------------------------------------- */

  /** JSON이면 {malwareType}/{type}, 문자열이면 ",Android.TestVirus" 꼬리에서 추출 */
  private String extractMalwareType(String payload) {
    if (!hasText(payload)) return null;

    // JSON 먼저
    try {
      JsonNode n = om.readTree(payload);
      if (n != null && n.isObject()) {
        if (n.hasNonNull("malwareType")) return n.get("malwareType").asText();
        if (n.hasNonNull("type"))        return n.get("type").asText();
        if (n.has("data") && n.get("data").hasNonNull("malwareType"))
          return n.get("data").get("malwareType").asText();
      }
    } catch (Exception ignored) {}

    // 문자열: ".../base.apk,Android.TestVirus\n"
    int comma = payload.lastIndexOf(',');
    if (comma >= 0 && comma < payload.length() - 1) {
      int end = payload.indexOf('\n', comma + 1);
      if (end < 0) end = payload.length();
      String cand = payload.substring(comma + 1, end).trim();
      if (cand.matches("[A-Za-z0-9._-]{2,}")) return cand;
    }
    return null;
  }

  /** "/com.xxx.yyy-xxxx/base.apk" 또는 "com.xxx.yyy" 토큰 추출 */
  private String extractMalwarePackage(String payload) {
    if (!hasText(payload)) return null;

    // JSON 먼저
    try {
      JsonNode n = om.readTree(payload);
      if (n != null && n.isObject()) {
        if (n.hasNonNull("malwarePackage")) return n.get("malwarePackage").asText();
        if (n.hasNonNull("package"))        return n.get("package").asText();
        if (n.hasNonNull("pkg"))            return n.get("pkg").asText();
        if (n.has("data") && n.get("data").hasNonNull("malwarePackage"))
          return n.get("data").get("malwarePackage").asText();
      }
    } catch (Exception ignored) {}

    // 경로 패턴
    Pattern p = Pattern.compile("/([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)(?:-[^/]+)?/base\\.apk");
    Matcher m = p.matcher(payload);
    if (m.find()) return m.group(1);

    // 토큰 fallback
    int idx = payload.indexOf("com.");
    if (idx >= 0) {
      int end = idx;
      while (end < payload.length()) {
        char c = payload.charAt(end);
        if (Character.isLetterOrDigit(c) || c == '.' || c == '_') end++;
        else break;
      }
      String cand = payload.substring(idx, end);
      if (cand.endsWith(".apk")) cand = cand.substring(0, cand.length() - 4);
      return cand;
    }
    return null;
  }
}
