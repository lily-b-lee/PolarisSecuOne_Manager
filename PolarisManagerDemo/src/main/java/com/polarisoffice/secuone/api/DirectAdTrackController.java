package com.polarisoffice.secuone.api;

import com.polarisoffice.secuone.dto.TrackEventReq;
import com.polarisoffice.secuone.service.PolarisDirectAdsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/directads")
public class DirectAdTrackController {

    private final PolarisDirectAdsService svc;

    public DirectAdTrackController(PolarisDirectAdsService svc) {
        this.svc = svc;
    }

    /** 노출 로그: views++, impressions 서브콜렉션, metrics_daily.views++ */
    @PostMapping("/{id}/track/impression")
    public ResponseEntity<Void> trackImpression(
            @PathVariable String id,
            @RequestParam(name = "detail", defaultValue = "true") boolean detail,
            @RequestHeader(name = "X-Customer-Code", required = false) String customerCode,
            @RequestBody(required = false) TrackEventReq body
    ) throws Exception {
        TrackEventReq req = body != null ? body : new TrackEventReq();
        // 필요하면 고객코드를 detail에 같이 남김
        if (customerCode != null && (req.clientId == null || req.clientId.isBlank())) {
            req.clientId = customerCode;
        }

        boolean ok = svc.trackImpression(id, req, detail);
        return ok ? ResponseEntity.noContent().build()
                  : ResponseEntity.notFound().build();
    }

    /** 클릭 로그: clicks++, clicks 서브콜렉션, metrics_daily.clicks++ */
    @PostMapping("/{id}/track/click")
    public ResponseEntity<Void> trackClick(
            @PathVariable String id,
            @RequestParam(name = "detail", defaultValue = "true") boolean detail,
            @RequestHeader(name = "X-Customer-Code", required = false) String customerCode,
            @RequestBody(required = false) TrackEventReq body
    ) throws Exception {
        TrackEventReq req = body != null ? body : new TrackEventReq();
        if (customerCode != null && (req.clientId == null || req.clientId.isBlank())) {
            req.clientId = customerCode;
        }

        boolean ok = svc.trackClick(id, req, detail);
        return ok ? ResponseEntity.noContent().build()
                  : ResponseEntity.notFound().build();
    }
}