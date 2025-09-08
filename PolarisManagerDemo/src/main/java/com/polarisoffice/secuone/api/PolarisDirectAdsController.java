package com.polarisoffice.secuone.api;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import com.polarisoffice.secuone.dto.DirectAdCreateReq;
import com.polarisoffice.secuone.dto.DirectAdRes;
import com.polarisoffice.secuone.dto.DirectAdUpdateReq;
import com.polarisoffice.secuone.dto.TrackEventReq;
import com.polarisoffice.secuone.service.PolarisDirectAdsService;

@RestController
@RequestMapping({"/directads", "/api/directads"})
public class PolarisDirectAdsController {
    private final PolarisDirectAdsService svc;
    public PolarisDirectAdsController(PolarisDirectAdsService svc) { this.svc = svc; }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> create(@Valid @RequestBody DirectAdCreateReq req) throws Exception {
        return ResponseEntity.ok(svc.create(req));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> update(@PathVariable String id, @RequestBody DirectAdUpdateReq req) throws Exception {
        return svc.update(id, req) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DirectAdRes> get(@PathVariable String id) throws Exception {
        var res = svc.get(id);
        return (res == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(res);
    }

    // ?limit=&status=&adType=&platform=
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DirectAdRes>> list(
            @RequestParam(defaultValue="60") int limit,
            @RequestParam(defaultValue="ALL") String status,
            @RequestParam(defaultValue="ALL") String adType,
            @RequestParam(required=false) String platform
    ) throws Exception {
        return ResponseEntity.ok(svc.list(limit, status, adType, platform));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
        return svc.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /* --- 트래킹 --- */

    /** 노출(뷰) 집계 + (detail=true면) 상세로그 기록 */
    @PostMapping("/{id}/impression")
    public ResponseEntity<Void> trackImpression(
            @PathVariable String id,
            @RequestParam(name = "detail", defaultValue = "false") boolean detail,
            @RequestBody(required = false) TrackEventReq body
    ) throws Exception {
        if (body == null) body = new TrackEventReq();
        boolean ok = svc.trackImpression(id, body, detail);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** 클릭 집계 + (detail=true면) 상세로그 기록 */
    @PostMapping("/{id}/click")
    public ResponseEntity<Void> trackClick(
            @PathVariable String id,
            @RequestParam(name = "detail", defaultValue = "false") boolean detail,
            @RequestBody(required = false) TrackEventReq body
    ) throws Exception {
        if (body == null) body = new TrackEventReq();
        boolean ok = svc.trackClick(id, body, detail);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** 현재 집계 조회 (views, clicks) */
    @GetMapping("/{id}/metrics")
    public ResponseEntity<Map<String, Long>> metrics(@PathVariable String id) throws Exception {
        Map<String, Long> m = svc.metrics(id);
        return (m == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(m);
    }


    @GetMapping(value="/_ping", produces = MediaType.TEXT_PLAIN_VALUE)
    public String ping() { return "ok"; }
}