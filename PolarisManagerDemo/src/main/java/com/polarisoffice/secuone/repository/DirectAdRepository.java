package com.polarisoffice.secuone.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.polarisoffice.secuone.dto.DirectAd;
import com.polarisoffice.secuone.dto.DirectAdRes;
import com.polarisoffice.secuone.dto.TrackEventReq;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.google.cloud.firestore.FieldValue.increment;
import static com.google.cloud.firestore.FieldValue.serverTimestamp;

@Repository
public class DirectAdRepository {

    private final Firestore db;
    private final String collectionName;

    public DirectAdRepository(Firestore db,
	        @Value("${directads.collection:PolarDirectAds}") String collectionName) {
	        this.db = db;
	        this.collectionName = collectionName;
	    }
    private CollectionReference col() { return db.collection(collectionName); }

    /* Create */
    public String create(DirectAd ad) throws ExecutionException, InterruptedException {
        DocumentReference doc = col().document(); // auto-ID
        ad.setId(doc.getId());
        doc.set(ad).get();
        return ad.getId();
    }

    /* Patch update (merge) */
    public boolean update(String id, Map<String, Object> patch) throws ExecutionException, InterruptedException {
        DocumentReference doc = col().document(id);
        if (!doc.get().get().exists()) return false;
        doc.set(patch, SetOptions.merge()).get();
        return true;
    }

    /* Get one */
    public DirectAdRes get(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot s = col().document(id).get().get();
        return s.exists() ? toRes(s) : null;
    }

    /* List with optional filters (no orderBy → no composite index need) */
    public List<DirectAdRes> list(int limit, String status, String adType)
            throws ExecutionException, InterruptedException {
        int lim = Math.max(1, Math.min(limit, 200));
        String st = norm(status);
        String at = norm(adType);

        Query q = col(); // ⬅️ 매번 얻기
        if ((status = norm(status)) != null) q = q.whereEqualTo("status", status);
        if ((adType = norm(adType)) != null) q = q.whereEqualTo("adType", adType);
        q = q.limit(Math.max(1, Math.min(limit, 200)));

        List<DirectAdRes> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : q.get().get().getDocuments()) out.add(toRes(d));
        return out;
    }

    /* List all (safety, no orderBy) */
    public List<DirectAdRes> listAll(int limit) throws ExecutionException, InterruptedException {
        int lim = Math.max(1, Math.min(limit, 200));
        List<DirectAdRes> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : col().limit(lim).get().get().getDocuments()) out.add(toRes(d));
        return out;
    }

    public boolean delete(String id) throws ExecutionException, InterruptedException {
        DocumentReference doc = col().document(id);
        if (!doc.get().get().exists()) return false;
        doc.delete().get();
        return true;
    }

    public Map<String, Long> metrics(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot s = col().document(id).get().get();
        if (!s.exists()) return null;
        Long views = s.getLong("viewCount");
        Long clicks = s.getLong("clickCount");
        Map<String, Long> m = new HashMap<>();
        m.put("views", views == null ? 0L : views);
        m.put("clicks", clicks == null ? 0L : clicks);
        return m;
    }

    public boolean incView(String id, TrackEventReq req, boolean logDetail)
            throws ExecutionException, InterruptedException {
        DocumentReference doc = col().document(id);
        if (!doc.get().get().exists()) return false;

        doc.update(Map.of("viewCount", increment(1), "updatedAt", serverTimestamp())).get();
        if (logDetail) {
            doc.collection("impressions").add(detailMap(req)).get();
            doc.collection("metrics_daily").document(todayKey())
               .set(Map.of("views", increment(1), "lastUpdated", serverTimestamp()), SetOptions.merge()).get();
        }
        return true;
    }

    public boolean incClick(String id, TrackEventReq req, boolean logDetail)
            throws ExecutionException, InterruptedException {
        DocumentReference doc = col().document(id);
        if (!doc.get().get().exists()) return false;

        doc.update(Map.of("clickCount", increment(1), "updatedAt", serverTimestamp())).get();
        if (logDetail) {
            doc.collection("clicks").add(detailMap(req)).get();
            doc.collection("metrics_daily").document(todayKey())
               .set(Map.of("clicks", increment(1), "lastUpdated", serverTimestamp()), SetOptions.merge()).get();
        }
        return true;
    }

    /* ---------- mapping helpers (lenient) ---------- */

    @SuppressWarnings("unchecked")
    private static DirectAdRes toRes(DocumentSnapshot d) {
        DirectAdRes r = new DirectAdRes();
        r.id             = d.getId();
        r.adType         = parseAdType(d.get("adType"));
        r.advertiserName = d.getString("advertiserName");
        r.backgroundColor= d.getString("backgroundColor");
        r.imageUrl       = d.getString("imageUrl");
        r.targetUrl      = d.getString("targetUrl");
        r.status         = parseStatus(d.get("status"));
        Object locales   = d.get("locales");
        if (locales instanceof List<?> list) r.locales = (List<String>) list;
        r.minAppVersion  = optStr(d.get("minAppVersion"));
        r.maxAppVersion  = optStr(d.get("maxAppVersion"));
        // publishedDate 또는 publishedAt 중 하나를 지원
        Object pub = d.get("publishedDate");
        if (pub == null) pub = d.get("publishedAt");
        r.publishedAt    = anyTsToInstant(pub);
        r.startAt        = anyTsToInstant(d.get("startAt"));
        r.endAt          = anyTsToInstant(d.get("endAt"));
        r.createdAt      = anyTsToInstant(d.get("createdAt"));
        r.updatedAt      = anyTsToInstant(d.get("updatedAt"));
        r.viewCount      = optLong(d.get("viewCount"));
        r.clickCount     = optLong(d.get("clickCount"));
        Object meta      = d.get("meta");
        if (meta instanceof Map<?,?> m) r.meta = (Map<String, Object>) m;
        return r;
    }

    private static String norm(String v) {
        if (v == null) return null;
        v = v.trim();
        if (v.isEmpty() || v.equalsIgnoreCase("all") || v.equals("*")) return null;
        return v.toUpperCase(Locale.ROOT);
    }

    private static String optStr(Object v) { return v == null ? null : String.valueOf(v); }
    private static Long optLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }

    private static Instant anyTsToInstant(Object o) {
        if (o == null) return null;
        if (o instanceof Timestamp t) return Instant.ofEpochSecond(t.getSeconds(), t.getNanos());
        if (o instanceof Date d) return d.toInstant();
        if (o instanceof String s) {
            try { return Instant.parse(s); } catch (Exception ignored) {}
        }
        return null;
    }

    private static Map<String, Object> detailMap(TrackEventReq r) {
        Map<String, Object> m = new HashMap<>();
        m.put("at", serverTimestamp());
        m.put("placement", r.placement);
        m.put("appVersion", r.appVersion);
        m.put("deviceModel", r.deviceModel);
        m.put("osVersion", r.osVersion);
        m.put("locale", r.locale);
        m.put("sessionId", r.sessionId);
        m.put("clientId", r.clientId);
        if (r.latitude != 0d || r.longitude != 0d) {
            m.put("latitude", r.latitude);
            m.put("longitude", r.longitude);
        }
        return m;
    }

    private static String todayKey() {
        return java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
    }

    // ---- tolerant enum parsing (handles legacy values like "EVENT_FAB") ----
    private static DirectAd.AdType parseAdType(Object v) {
        if (v == null) return null;
        if (v instanceof DirectAd.AdType t) return t;
        String s = String.valueOf(v).trim().toUpperCase(Locale.ROOT);
        // alias
        if (s.equals("EVENT_FAB") || s.equals("FAB")) s = "FLOATING_FAB";
        try { return DirectAd.AdType.valueOf(s); } catch (Exception e) { return null; }
    }

    private static DirectAd.AdStatus parseStatus(Object v) {
        if (v == null) return null;
        if (v instanceof DirectAd.AdStatus t) return t;
        String s = String.valueOf(v).trim().toUpperCase(Locale.ROOT);
        try { return DirectAd.AdStatus.valueOf(s); } catch (Exception e) { return null; }
    }
}
