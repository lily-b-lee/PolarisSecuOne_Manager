// src/main/java/com/polarisoffice/secuone/api/TrackEventController.java
package com.polarisoffice.secuone.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarisoffice.secuone.domain.CustomerEntity;
import com.polarisoffice.secuone.domain.EventLogEntity;
import com.polarisoffice.secuone.domain.SecurityEventEntity;
import com.polarisoffice.secuone.repository.CustomerRepository;
import com.polarisoffice.secuone.repository.EventLogRepository;
import com.polarisoffice.secuone.repository.SecurityEventRepository;
import com.polarisoffice.secuone.service.PolarisDirectAdsService;

// ✅ 서비스 DTO (광고 트래킹용)
import com.polarisoffice.secuone.dto.TrackEventReq;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/track")
public class TrackEventController {

  private static final String T_MALWARE = "MALWARES_APP";

  private final EventLogRepository eventLogs;
  private final SecurityEventRepository secRepo;
  private final CustomerRepository customers;
  private final ObjectMapper om;
  private final PolarisDirectAdsService directAdsService;

  public TrackEventController(
      EventLogRepository eventLogs,
      SecurityEventRepository secRepo,
      CustomerRepository customers,
      ObjectMapper om,
      PolarisDirectAdsService directAdsService
  ) {
    this.eventLogs = eventLogs;
    this.secRepo = secRepo;
    this.customers = customers;
    this.om = om;
    this.directAdsService = directAdsService;
  }

  /** 앱에서 오는 요청 바디 */
  public static class TrackEventIn {
    public String action;       // e.g. "ad_impression", "ad_click", "security_event" ...
    public String objectType;   // e.g. "direct_ad", "MALWARES_APP" ...
    public String objectId;     // e.g. 광고 id / 디바이스 id
    public Map<String, String> extra; // 부가 정보(문자열 맵)
  }

  /** 응답 바디: 공통 event_logs id 반환 */
  public static class TrackEventRes {
    public Long id;
    public TrackEventRes(Long id) { this.id = id; }
  }

  /** 단일 엔드포인트: 공통 로그 + (필요시) 타입별 라우팅 */
  @PostMapping("/events")
  public ResponseEntity<?> create(
      @RequestHeader(name = "X-Customer-Code", required = false) String customerCode, // ✅ 선택
      @RequestBody TrackEventIn in,
      HttpServletRequest http
  ) throws Exception {

    // 1) 고객 연결(있으면), 없으면 null 그대로 저장
    CustomerEntity cust = null;
    if (customerCode != null && !customerCode.isBlank()) {
      cust = customers.findByCodeIgnoreCase(customerCode).orElse(null);
    }

    // 2) 공통 event_logs 적재
    var log = new EventLogEntity();
    log.setCustomer(cust);
    log.setAction(in.action);
    log.setObjectType(in.objectType);
    log.setObjectId(in.objectId);
    log.setActor(null);
    log.setIp(http.getRemoteAddr());
    log.setUa(http.getHeader("User-Agent"));
    log.setMemo(toJson(in.extra));  // extra 전체 저장
    log.setCreatedAt(Instant.now());
    eventLogs.save(log);

    // 3) 타입별 라우팅
    final String type   = safe(in.objectType);
    final String action = safe(in.action);

    // 3-1) 직광고: customerCode 불필요. Firestore 카운트 증가만 수행.
    if ("DIRECT_AD".equals(type)) {
      TrackEventReq req = toDirectAdReq(in.extra); // 고객코드 사용 안 함
      switch (action) {
        case "AD_IMPRESSION" -> directAdsService.trackImpression(in.objectId, req, true);
        case "AD_CLICK"      -> directAdsService.trackClick(in.objectId, req, true);
        default -> {
          return ResponseEntity.badRequest().body(Map.of("message", "unsupported action for direct_ad"));
        }
      }
      // 광고의 경우에도 공통 로그 id를 반환 (클라 호환)
      return ResponseEntity.ok(new TrackEventRes(log.getId()));
    }

    // 3-2) (선택) 보안 이벤트 미러링: 기존 로직 유지
    if (equalsIgnoreCase(in.objectType, T_MALWARE)) {
      String raw = in.extra == null ? null : in.extra.get("payload");
      MalwareInfo mi = parseMalwareInfo(raw);

      var se = new SecurityEventEntity();
      se.setCustomerCode(cust != null ? cust.getCode() : null);
      se.setDeviceId(in.objectId);
      se.setEventType(T_MALWARE);
      se.setSourcePackage(in.extra == null ? null : in.extra.get("sourcePackage"));
      se.setSourceDomain(null);

      Map<String,Object> memo = new LinkedHashMap<>();
      if (mi.pkg  != null) memo.put("malwarePackage", mi.pkg);
      if (mi.type != null) memo.put("malwareType", mi.type);
      if (raw != null)     memo.put("raw", raw);
      se.setPayloadJson(toJson(memo));
      se.setCreatedAt(Instant.now());
      secRepo.save(se);
    }

    // 기본 응답(공통 로그 id)
    return ResponseEntity.ok(new TrackEventRes(log.getId()));
  }

  /* ----------------- helpers ----------------- */

  private static String safe(String s) { return s == null ? "" : s.trim().toUpperCase(); }
  private static boolean equalsIgnoreCase(String a, String b) {
    return a != null && b != null && a.equalsIgnoreCase(b);
  }

  /** extra → 광고 서비스 DTO (customerCode 사용 안 함) */
  private static TrackEventReq toDirectAdReq(Map<String, String> extra) {
    TrackEventReq r = new TrackEventReq(); // com.polarisoffice.secuone.dto.TrackEventReq (public fields)
    if (extra != null) {
      r.placement   = extra.getOrDefault("placement", null);
      r.appVersion  = extra.getOrDefault("appVersion", null);
      r.deviceModel = extra.getOrDefault("deviceModel", null);
      r.osVersion   = extra.getOrDefault("osVersion", null);
      r.locale      = extra.getOrDefault("locale", null);
      r.sessionId   = extra.getOrDefault("sessionId", null);
      r.clientId    = extra.getOrDefault("clientId", null);
      try {
        if (extra.get("latitude")  != null) r.latitude  = Double.parseDouble(extra.get("latitude"));
        if (extra.get("longitude") != null) r.longitude = Double.parseDouble(extra.get("longitude"));
      } catch (NumberFormatException ignore) {}
    }
    return r;
  }

  private String toJson(Object obj) {
    try { return obj == null ? null : om.writeValueAsString(obj); }
    catch (Exception e) { return null; }
  }

  // -------- 악성앱 정보 파서(기존 유지) --------
  private static class MalwareInfo { final String pkg, type; MalwareInfo(String p, String t){ pkg=p; type=t; } }

  private static MalwareInfo parseMalwareInfo(String payload) {
    if (payload == null) return new MalwareInfo(null, null);

    // "/.../com.xxx.yyy-XXXXX/base.apk,Android.TestVirus\n" 형태
    Matcher m = Pattern
        .compile("/([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)(?:-[^/]+)?/base\\.apk,\\s*([^\\s,\\n]+)")
        .matcher(payload);
    if (m.find()) return new MalwareInfo(m.group(1), m.group(2));

    // JSON 백업
    try {
      var node = new ObjectMapper().readTree(payload);
      String pkg = node.has("malwarePackage") ? node.get("malwarePackage").asText(null)
                 : node.has("pkg") ? node.get("pkg").asText(null)
                 : node.has("packageName") ? node.get("packageName").asText(null)
                 : null;
      String type = node.has("malwareType") ? node.get("malwareType").asText(null)
                  : node.has("type") ? node.get("type").asText(null)
                  : null;
      if (pkg != null || type != null) return new MalwareInfo(pkg, type);
    } catch (Exception ignore){}

    // 최후: 'com.' 토큰 + 콤마 뒤 한 단어
    Matcher mPkg  = Pattern.compile("\\b(com[\\w.]+)\\b").matcher(payload);
    Matcher mType = Pattern.compile(",\\s*([^,\\s\\n]+)").matcher(payload);
    String pkg  = mPkg.find()  ? mPkg.group(1)  : null;
    String type = mType.find() ? mType.group(1) : null;
    return new MalwareInfo(pkg, type);
  }
}
