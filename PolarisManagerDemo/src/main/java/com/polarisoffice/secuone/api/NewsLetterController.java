package com.polarisoffice.secuone.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.polarisoffice.secuone.dto.*;
import com.polarisoffice.secuone.service.NewsletterService;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping({"/secu-news", "/newsletters"})
public class NewsLetterController {

  private final NewsletterService svc;
  public NewsLetterController(NewsletterService svc) { this.svc = svc; }

  @GetMapping(value="/{id}", produces="application/json")
  public ResponseEntity<NewsletterRes> get(@PathVariable String id) throws Exception {
    var res = svc.get(id);
    return (res == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(res);
  }

  @GetMapping(produces="application/json")
  public ResponseEntity<List<NewsletterRes>> list(@RequestParam(defaultValue = "20") int limit) throws Exception {
    int safe = Math.max(1, Math.min(limit, 100));
    return ResponseEntity.ok(svc.listLatest(safe));
  }

  @PostMapping(consumes="application/json")
  public ResponseEntity<String> create(@Valid @RequestBody NewsletterCreateReq req) throws Exception {
    String id = svc.create(req);
    // 201 Created + Location 헤더
    return ResponseEntity.created(URI.create("/newsletters/" + id)).body(id);
  }

  // 살아있는지 확인용
  @GetMapping("/_ping")
  public String ping() { return "ok"; }
}
