package com.polarisoffice.secuone.api;

import com.polarisoffice.secuone.domain.NoticeCategory;
import com.polarisoffice.secuone.dto.*;
import com.polarisoffice.secuone.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
// 프런트 탐지 후보 전부 지원: /api/notices, /notices, (호환) /api/notice, /notice
@RequestMapping({"/api/notices", "/notices", "/api/notice", "/notice"})
public class NoticeController {

  private final NoticeService svc;
  public NoticeController(NoticeService svc) { this.svc = svc; }

  /** ping: notice.js의 CANDIDATES + "/_ping" 과 매칭 */
  @GetMapping("/_ping")
  public String ping() { return "ok"; }

  /** 목록 조회: GET /api/notices?limit=&category=  */
  @GetMapping(produces = "application/json")
  public ResponseEntity<List<NoticeRes>> list(
      @RequestParam(defaultValue = "20") Integer limit,
      @RequestParam(required = false) NoticeCategory category
  ) throws Exception {
    int safe = Math.max(1, Math.min(limit == null ? 20 : limit, 100));
    return ResponseEntity.ok(svc.listLatest(safe, category));
  }

  /** 단건 조회: GET /api/notices/{id} */
  @GetMapping(value = "/{id}", produces = "application/json")
  public ResponseEntity<NoticeRes> get(@PathVariable String id) throws Exception {
    var res = svc.get(id);
    return res == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(res);
  }

  /** 생성: POST /api/notices?sendPush=&test=  (프런트의 쿼리 키와 맞춤) */
  @PostMapping(consumes = "application/json")
  public ResponseEntity<String> create(
      @Valid @RequestBody NoticeCreateReq req,
      @RequestParam(value = "sendPush", defaultValue = "false") boolean sendPush,
      @RequestParam(value = "test",     defaultValue = "false") boolean test   // 필요 시 서비스에 반영
  ) throws Exception {
    // 기존 코드가 push 값을 무시하던 문제 수정
    String id = svc.createAndPush(req, sendPush); // test도 쓰려면 서비스 시그니처 확장
    return ResponseEntity.created(URI.create("/api/notices/" + id)).body(id);
  }

  /** 수정: PUT /api/notices/{id} */
  @PutMapping(value = "/{id}", consumes = "application/json")
  public ResponseEntity<Void> update(@PathVariable String id, @RequestBody NoticeUpdateReq req) throws Exception {
    return svc.update(id, req) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  /** 삭제: DELETE /api/notices/{id} */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
    return svc.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }
}
