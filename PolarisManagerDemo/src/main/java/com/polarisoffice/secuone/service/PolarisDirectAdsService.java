package com.polarisoffice.secuone.service;

import com.google.cloud.Timestamp;
import com.polarisoffice.secuone.dto.DirectAd;
import com.polarisoffice.secuone.dto.DirectAdCreateReq;
import com.polarisoffice.secuone.dto.DirectAdRes;
import com.polarisoffice.secuone.dto.DirectAdUpdateReq;
import com.polarisoffice.secuone.dto.TrackEventReq;
import com.polarisoffice.secuone.repository.DirectAdRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

import static com.google.cloud.firestore.FieldValue.serverTimestamp;

@Service
public class PolarisDirectAdsService {

    private final DirectAdRepository repo;

    public PolarisDirectAdsService(DirectAdRepository repo) {
        this.repo = repo;
    }

    /* Create */
    public String create(DirectAdCreateReq req) throws Exception {
        Timestamp now = Timestamp.now();

        DirectAd ad = new DirectAd();
        ad.setAdType(req.getAdType());
        ad.setAdvertiserName(req.getAdvertiserName());
        ad.setBackgroundColor(req.getBackgroundColor());
        ad.setImageUrl(req.getImageUrl());
        ad.setTargetUrl(req.getTargetUrl());
        ad.setStatus(req.getStatus());
        ad.setLocales(req.getLocales());
        ad.setMinAppVersion(req.getMinAppVersion());
        ad.setMaxAppVersion(req.getMaxAppVersion());
        ad.setPublishedAt(toTs(req.getPublishedAt()));
        ad.setStartAt(toTs(req.getStartAt()));
        ad.setEndAt(toTs(req.getEndAt()));
        ad.setCreatedAt(now);
        ad.setUpdatedAt(now);
        ad.setViewCount(0);
        ad.setClickCount(0);
        ad.setMeta(req.getMeta());

        return repo.create(ad);
    }

    /* Update (merge) */
    public boolean update(String id, DirectAdUpdateReq req) throws Exception {
        Map<String, Object> p = new HashMap<>();
        if (req.getAdType() != null)          p.put("adType", req.getAdType().name());
        if (req.getAdvertiserName() != null)  p.put("advertiserName", req.getAdvertiserName());
        if (req.getBackgroundColor() != null) p.put("backgroundColor", req.getBackgroundColor());
        if (req.getImageUrl() != null)        p.put("imageUrl", req.getImageUrl());
        if (req.getTargetUrl() != null)       p.put("targetUrl", req.getTargetUrl());
        if (req.getStatus() != null)          p.put("status", req.getStatus().name());
        if (req.getLocales() != null)         p.put("locales", req.getLocales());
        if (req.getMinAppVersion() != null)   p.put("minAppVersion", req.getMinAppVersion());
        if (req.getMaxAppVersion() != null)   p.put("maxAppVersion", req.getMaxAppVersion());
        if (req.getPublishedAt() != null)     p.put("publishedAt", toTs(req.getPublishedAt()));
        if (req.getStartAt() != null)         p.put("startAt", toTs(req.getStartAt()));
        if (req.getEndAt() != null)           p.put("endAt", toTs(req.getEndAt()));
        if (req.getMeta() != null)            p.put("meta", req.getMeta());
        p.put("updatedAt", serverTimestamp());

        return repo.update(id, p);
    }

    /* Get */
    public DirectAdRes get(String id) throws Exception { return repo.get(id); }

    /* List (controller 호환 시그니처 유지) */
    public List<DirectAdRes> list(int limit, String status, String adType, String platformIgnored) throws Exception {
        List<DirectAdRes> out = repo.list(limit, status, adType);
        if (out.isEmpty() && ((status != null && !status.isBlank()) || (adType != null && !adType.isBlank()))) {
            // 필터 0건이면 UX 위해 전체로 폴백 (선택)
            return repo.listAll(limit);
        }
        return out;
    }

    public boolean delete(String id) throws Exception { return repo.delete(id); }

    public Map<String, Long> metrics(String id) throws Exception { return repo.metrics(id); }

    public boolean trackImpression(String id, TrackEventReq req, boolean logDetail) throws Exception {
	    if (req == null) req = new TrackEventReq();
	    return repo.incView(id, req, logDetail);
    }
    
    public boolean trackClick(String id, TrackEventReq req, boolean logDetail) throws Exception {
	   if (req == null) req = new TrackEventReq();
	   return repo.incClick(id, req, logDetail);
    }
    /* helper */
    private static Timestamp toTs(Instant i) {
        return (i == null) ? null : Timestamp.ofTimeSecondsAndNanos(i.getEpochSecond(), i.getNano());
    }
    
    
}
