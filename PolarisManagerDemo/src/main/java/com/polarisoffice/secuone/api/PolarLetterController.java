package com.polarisoffice.secuone.api;

import com.polarisoffice.secuone.dto.*;
import com.polarisoffice.secuone.service.PolarLetterService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// ← 프런트가 /polarletters 또는 /api/polarletters 둘 다 시도하므로 둘 다 열어줍니다.
@RequestMapping({"/polarletters", "/api/polarletters"})
public class PolarLetterController {

    private final PolarLetterService svc;
    public PolarLetterController(PolarLetterService svc) { this.svc = svc; }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> create(@Valid @RequestBody PolarLetterCreateReq req) throws Exception {
        return ResponseEntity.ok(svc.create(req));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> update(@PathVariable String id, @RequestBody PolarLetterUpdateReq req) throws Exception {
        boolean ok = svc.update(id, req);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PolarLetterRes> get(@PathVariable String id) throws Exception {
        var res = svc.get(id);
        return (res == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(res);
    }

    // ?limit=&author=  → limit 보정(1~100) + JSON 명시
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PolarLetterRes>> list(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String author
    ) throws Exception {
        int lim = Math.max(1, Math.min(limit, 100));
        return ResponseEntity.ok(svc.listLatest(lim, author));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
        boolean ok = svc.delete(id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // 프런트가 살아있음 확인용으로 호출
    @GetMapping(value = "/_ping", produces = MediaType.TEXT_PLAIN_VALUE)
    public String ping() { return "ok"; }
}
