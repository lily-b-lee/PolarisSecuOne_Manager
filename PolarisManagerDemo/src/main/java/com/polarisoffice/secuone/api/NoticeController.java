package com.polarisoffice.secuone.api;


import com.polarisoffice.secuone.domain.NoticeCategory;
import com.polarisoffice.secuone.dto.*;
import com.polarisoffice.secuone.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api")
public class NoticeController {

	  private final NoticeService svc;
	  public NoticeController(NoticeService svc) { this.svc = svc; }

	  @PostMapping("/notices")
	  public ResponseEntity<String> create(
	      @Valid @RequestBody NoticeCreateReq req,
	      @RequestParam(defaultValue = "true") boolean push
	  ) throws Exception {
	      boolean pushb = false; // 필요하면 true
	       var result = svc.createAndPush(req, pushb);
	       return ResponseEntity.ok(result);
	  }

	  @PutMapping("/{id}")
	  public ResponseEntity<Void> update(@PathVariable String id, @RequestBody NoticeUpdateReq req) throws Exception {
	    boolean ok = svc.update(id, req);
	    return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	  }

	  @GetMapping("/{id}")
	  public ResponseEntity<NoticeRes> get(@PathVariable String id) throws Exception {
	    var res = svc.get(id);
	    return res == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(res);
	  }

	  // ?limit=&category=  (category는 EVENT|EMERGENCY|SERVICE_GUIDE|UPDATE)
	  @GetMapping
	  public ResponseEntity<List<NoticeRes>> list(
	      @RequestParam(defaultValue = "20") Integer limit,
	      @RequestParam(required = false) NoticeCategory category
	  ) throws Exception {
	    return ResponseEntity.ok(svc.listLatest(limit, category));
	  }

	  @DeleteMapping("/{id}")
	  public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
	    return svc.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	  }

	  @GetMapping("/_ping")
	  public String ping() { return "ok"; }
}
