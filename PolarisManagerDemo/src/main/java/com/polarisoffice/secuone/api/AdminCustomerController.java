package com.polarisoffice.secuone.api;

import com.polarisoffice.secuone.domain.CustomerEntity;
import com.polarisoffice.secuone.domain.SettlementEntity;
import com.polarisoffice.secuone.dto.Customers;
import com.polarisoffice.secuone.repository.CustomerRepository;
import com.polarisoffice.secuone.repository.SettlementRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 관리자 - 고객사 관리 + 통계 (PK=code 기준)
 */
@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

  private final CustomerRepository repo;
  private final SettlementRepository settlementRepo;

  public AdminCustomerController(CustomerRepository repo, SettlementRepository settlementRepo) {
    this.repo = repo;
    this.settlementRepo = settlementRepo;
  }

  // --------------------------------
  // 기본 CRUD
  // --------------------------------

  /** 목록/검색 (q 없으면 전체, code 오름차순) */
  @GetMapping
  public List<Customers.Res> list(@RequestParam(value = "q", required = false) String q) {
    var list = (q == null || q.isBlank())
        ? repo.findAll(Sort.by(Sort.Direction.ASC, "code"))
        : repo.search(q.trim());
    return list.stream().map(this::toRes).toList();
  }

  /** 단건 (PK=code) */
  @GetMapping("/{code}")
  public ResponseEntity<Customers.Res> get(@PathVariable String code) {
    return repo.findById(code)
        .map(c -> ResponseEntity.ok(toRes(c)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /** 코드 중복 체크 */
  @GetMapping("/exists")
  public Map<String, Object> exists(@RequestParam("code") String code) {
    boolean exists = repo.existsByCode(code);
    return Map.of("code", code, "exists", exists);
  }

  /** 생성 */
  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody Customers.CreateReq in) {
    String code = in.getCode().trim().toLowerCase();
    if (repo.existsByCode(code)) {
      return ResponseEntity.status(409).body(Map.of("message", "이미 존재하는 코드입니다."));
    }

    var c = new CustomerEntity();
    c.setCode(code);
    c.setName(in.getName());
    c.setIntegrationType(in.getIntegrationType());
    if (in.getRsPercent() != null) c.setRsPercent(in.getRsPercent());
    if (in.getCpiValue()  != null) c.setCpiValue(in.getCpiValue());
    c.setNote(in.getNote());

    c = repo.save(c);
    return ResponseEntity.ok(toRes(c));
  }

  /** 수정(부분 업데이트) — PK=code */
  @PatchMapping("/{code}")
  public ResponseEntity<?> update(@PathVariable String code, @Valid @RequestBody Customers.UpdateReq in) {
    var opt = repo.findById(code);
    if (opt.isEmpty()) return ResponseEntity.notFound().build();
    var c = opt.get();

    if (in.getName()            != null) c.setName(in.getName());
    if (in.getIntegrationType() != null) c.setIntegrationType(in.getIntegrationType());
    if (in.getRsPercent()       != null) c.setRsPercent(in.getRsPercent());
    if (in.getCpiValue()        != null) c.setCpiValue(in.getCpiValue());
    if (in.getNote()            != null) c.setNote(in.getNote());

    c = repo.save(c);
    return ResponseEntity.ok(toRes(c));
  }

  /** 삭제 — PK=code */
  @DeleteMapping("/{code}")
  public ResponseEntity<?> delete(@PathVariable String code) {
    if (!repo.existsById(code)) return ResponseEntity.notFound().build();
    repo.deleteById(code);
    return ResponseEntity.noContent().build();
  }

  // --------------------------------
  // 통계 API (월별 다운로드/삭제/청구금액)
  // --------------------------------

  /** 월별 응답(통화 표시) */
  public record MonthlyRes(
      String month,
      long downloads,
      long deletes,
      BigDecimal amountDue,
      String currency
  ) {}

  /** 전체 통계 응답 */
  public record StatsRes(
      String code,
      String name,
      long totalDownloads,
      long totalDeletes,
      BigDecimal totalAmountDue,
      List<MonthlyRes> monthly
  ) {}

  /**
   * GET /api/admin/customers/{code}/stats?from=YYYY-MM&to=YYYY-MM
   * from/to 없으면 저장소 구현에서 기본 처리(예: 최근 12개월)
   */
  @GetMapping("/{code}/stats")
  public ResponseEntity<?> stats(@PathVariable String code,
                                 @RequestParam(required = false) String fromMonth,
                                 @RequestParam(required = false) String toMonth) {
    var opt = repo.findById(code);
    if (opt.isEmpty()) return ResponseEntity.notFound().build();
    var c = opt.get();

    // SettlementRepository: code 기준 조회
    var rows = settlementRepo.findByFiltersCode(code, fromMonth, toMonth);

    long totalDownloads = 0L;
    long totalDeletes   = 0L;
    BigDecimal totalAmount = BigDecimal.ZERO;
    List<MonthlyRes> monthly = new ArrayList<>();

    for (SettlementEntity s : rows) {
      long d   = nzl(s.getDownloads());
      long del = nzl(s.getDeletes());
      BigDecimal amt = nzbd(s.getTotalAmount());

      totalDownloads += d;
      totalDeletes   += del;
      totalAmount    = totalAmount.add(amt);

      monthly.add(new MonthlyRes(
          s.getSettleMonth(),
          d,
          del,
          amt,
          s.getCurrency()   // 엔티티에 status 없음 → currency 사용
      ));
    }

    var res = new StatsRes(
        c.getCode(), c.getName(),
        totalDownloads, totalDeletes, totalAmount,
        monthly
    );
    return ResponseEntity.ok(res);
  }

  // ====== mapper ======
  private Customers.Res toRes(CustomerEntity c) {
    // 최신 DTO 구조: (code, name, integrationType, rsPercent, cpiValue, note, createdAt, updatedAt)
    return new Customers.Res(
        c.getCode(),
        c.getName(),
        c.getIntegrationType(),
        c.getRsPercent(),
        c.getCpiValue(),
        c.getNote(),
        c.getCreatedAt(),
        c.getUpdatedAt()
    );
  }

  //--- NPE 방지 헬퍼 ---
  private static long nzl(Long v) { return v == null ? 0L : v; }
  private static BigDecimal nzbd(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
