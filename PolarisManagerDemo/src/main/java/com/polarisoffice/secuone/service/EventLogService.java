package com.polarisoffice.secuone.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarisoffice.secuone.domain.CustomerEntity;
import com.polarisoffice.secuone.domain.EventLogEntity;
import com.polarisoffice.secuone.repository.EventLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventLogService {
  private final EventLogRepository repo;
  private final ObjectMapper om;

  @PersistenceContext
  private EntityManager em;

  public EventLogService(EventLogRepository repo, ObjectMapper om) {
    this.repo = repo; this.om = om;
  }

  /* ---------- 공통 저장/빌더 ---------- */

  public EventLogEntity save(EventLogEntity e) { return repo.save(e); }

  public EventLogEntity buildFrom(HttpServletRequest req,
                                  String action, String objectType, String objectId,
                                  Map<String, Object> extraMemo,
                                  CustomerEntity customer) {
    EventLogEntity e = new EventLogEntity();
    e.setCustomer(customer); // ✅ 필수
    e.setAction(action);
    e.setObjectType(objectType);
    e.setObjectId(objectId);
    e.setActor(Optional.ofNullable(req.getHeader("X-User-Id"))
        .orElse((String) req.getAttribute("userId")));
    e.setIp(Optional.ofNullable(req.getHeader("X-Forwarded-For"))
        .map(s -> s.isBlank() ? null : s.split(",")[0].trim())
        .orElse(req.getRemoteAddr()));
    e.setUa(req.getHeader("User-Agent"));
    try {
      e.setMemo(extraMemo == null ? null : om.writeValueAsString(extraMemo));
    } catch (Exception ignore) { e.setMemo(null); }
    e.setCreatedAt(Instant.now());
    return e;
  }

  /* ---------- Specification (기존) ---------- */

  public Specification<EventLogEntity> spec(String action, String objectType, String objectId,
                                            String actor, Instant from, Instant to) {
    return (root, q, cb) -> {
      var p = cb.conjunction();
      if (action != null)     p = cb.and(p, cb.equal(root.get("action"), action));
      if (objectType != null) p = cb.and(p, cb.equal(root.get("objectType"), objectType));
      if (objectId != null)   p = cb.and(p, cb.equal(root.get("objectId"), objectId));
      if (actor != null)      p = cb.and(p, cb.equal(root.get("actor"), actor));
      if (from != null)       p = cb.and(p, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
      if (to != null)         p = cb.and(p, cb.lessThanOrEqualTo(root.get("createdAt"), to));
      return p;
    };
  }

  /* ---------- 고객 코드 + 기간 필터 전용 Spec ---------- */

  public Specification<EventLogEntity> specByCustomerAndRange(
      String customerCode, String objectType, Instant from, Instant to
  ) {
    return (root, q, cb) -> {
      var cust = root.join("customer"); // ManyToOne(CustomerEntity)
      var p = cb.equal(cb.upper(cust.get("code")), customerCode.toUpperCase());
      if (objectType != null) p = cb.and(p, cb.equal(root.get("objectType"), objectType));
      if (from != null)       p = cb.and(p, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
      if (to != null)         p = cb.and(p, cb.lessThanOrEqualTo(root.get("createdAt"), to));
      return p;
    };
  }

  /* ---------- 조회 메서드 (대시보드/상세용) ---------- */

  public Page<EventLogEntity> findPageByCustomer(
      String customerCode, String objectType, Instant from, Instant to, int page, int size
  ) {
    var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    return repo.findAll(specByCustomerAndRange(customerCode, objectType, from, to), pageable);
  }

  public List<EventLogEntity> findRecentByCustomer(
      String customerCode, String objectType, Instant from, Instant to, int limit
  ) {
    return repo.findAll(
            specByCustomerAndRange(customerCode, objectType, from, to),
            Sort.by(Sort.Direction.DESC, "createdAt"))
        .stream().limit(limit).toList();
  }

  /** objectType 별 집계: MALWARES_APP / ROOTING_DETECTED / REMOTE_CONTROL_APP */
  public Map<String, Long> countByObjectType(String customerCode, Instant from, Instant to) {
    var cb = em.getCriteriaBuilder();
    var cq = cb.createQuery(Object[].class);
    var root = cq.from(EventLogEntity.class);

    var p = cb.equal(cb.upper(root.get("customer").get("code")), customerCode.toUpperCase());
    if (from != null) p = cb.and(p, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
    if (to != null)   p = cb.and(p, cb.lessThanOrEqualTo(root.get("createdAt"), to));

    cq.multiselect(root.get("objectType"), cb.count(root))
      .where(p)
      .groupBy(root.get("objectType"));

    List<Object[]> rows = em.createQuery(cq).getResultList();
    return rows.stream().collect(Collectors.toMap(
        r -> (String) r[0],
        r -> ((Number) r[1]).longValue()
    ));
  }

  /* ---------- 편의 유틸 ---------- */

  public static Instant startOfDay(LocalDate d, ZoneId zone) {
    return d.atStartOfDay(zone).toInstant();
  }
  public static Instant endOfDay(LocalDate d, ZoneId zone) {
    return d.plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant();
  }

  /** memo(JSON)을 Map으로 파싱 (뷰에 세부정보 뿌릴 때 사용) */
  public Map<String, Object> parseMemo(EventLogEntity e) {
    if (e.getMemo() == null || e.getMemo().isBlank()) return Map.of();
    try {
      return om.readValue(e.getMemo(), new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      return Map.of("raw", e.getMemo());
    }
  }

  /** 대시보드용 간단 요약 구조체 (원하면 Controller에서 그대로 반환/모델로 사용) */
  public record DashboardSummary(
      String customerCode,
      Instant from, Instant to,
      long total, long malware, long rooting, long remote,
      List<EventLogEntity> recentMalware,
      List<EventLogEntity> recentRooting
  ) {}

  public DashboardSummary buildSummary(String customerCode, Instant from, Instant to) {
    var counts = countByObjectType(customerCode, from, to);
    long malware = counts.getOrDefault("MALWARES_APP", 0L);
    long rooting = counts.getOrDefault("ROOTING_DETECTED", 0L);
    long remote  = counts.getOrDefault("REMOTE_CONTROL_APP", 0L);
    long total   = malware + rooting + remote;

    var recentMalware = findRecentByCustomer(customerCode, "MALWARES_APP", from, to, 100);
    var recentRooting = findRecentByCustomer(customerCode, "ROOTING_DETECTED", from, to, 100);

    return new DashboardSummary(customerCode.toUpperCase(), from, to,
        total, malware, rooting, remote, recentMalware, recentRooting);
  }
}
