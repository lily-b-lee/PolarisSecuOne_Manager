package com.polarisoffice.secuone.service;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.polarisoffice.secuone.dto.PolarLetterCreateReq;
import com.polarisoffice.secuone.dto.PolarLetterRes;
import com.polarisoffice.secuone.dto.PolarLetterUpdateReq;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PolarLetterService {

    private static final String COLLECTION = "polarLetter"; // ← 콘솔과 동일
    private static final DateTimeFormatter DOT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private CollectionReference col() {
        Firestore db = FirestoreClient.getFirestore();
        return db.collection(COLLECTION);
    }

    private String todayDot() { return LocalDate.now().format(DOT); }

    /* ---------- Create ---------- */
    public String create(PolarLetterCreateReq req) throws Exception {
        Map<String, Object> data = new HashMap<>();
        if (req.author() != null && !req.author().isBlank()) data.put("author", req.author().trim());
        data.put("title", req.title().trim());
        if (req.content() != null && !req.content().isBlank()) data.put("content", req.content().trim());
        if (req.url() != null && !req.url().isBlank()) data.put("url", req.url().trim());
        if (req.thumbnail() != null && !req.thumbnail().isBlank()) data.put("thumbnail", req.thumbnail().trim());
        data.put("create_time", (req.createTime() == null || req.createTime().isBlank())
                ? todayDot() : req.createTime().trim());
        data.put("updatedAt", java.time.OffsetDateTime.now().toString());

        DocumentReference doc = col().document(); // auto-ID
        doc.set(data).get();
        return doc.getId();
    }

    /* ---------- Update (partial) ---------- */
    public boolean update(String id, PolarLetterUpdateReq req) throws Exception {
        Map<String, Object> patch = new HashMap<>();
        if (req.author()     != null) patch.put("author", req.author().trim());
        if (req.title()      != null) patch.put("title", req.title().trim());
        if (req.content()    != null) patch.put("content", req.content().trim());
        if (req.url()        != null) patch.put("url", req.url().trim());
        if (req.thumbnail()  != null) patch.put("thumbnail", req.thumbnail().trim());
        if (req.createTime() != null) patch.put("create_time", req.createTime().trim());
        if (patch.isEmpty()) return false;

        patch.put("updatedAt", java.time.OffsetDateTime.now().toString());
        var wr = col().document(id).update(patch).get();
        return wr.getUpdateTime() != null;
    }

    /* ---------- Read (one) ---------- */
    public PolarLetterRes get(String id) throws Exception {
        DocumentSnapshot d = col().document(id).get().get();
        if (!d.exists()) return null;
        return toRes(d);
    }

    /* ---------- Read (list) : latest, optional author filter ---------- */
    public List<PolarLetterRes> listLatest(Integer limit, String author) throws Exception {
        int n = Math.max(1, Math.min(limit == null ? 20 : limit, 100));
        try {
            Query q = col();
            if (author != null && !author.isBlank()) q = q.whereEqualTo("author", author);
            // create_time 은 문자열("yyyy.MM.dd") — 고정 포맷이면 문자열 정렬로 최신순 OK
            q = q.orderBy("create_time", Query.Direction.DESCENDING).limit(n);
            QuerySnapshot qs = q.get().get();
            return qs.getDocuments().stream().map(this::toRes).collect(Collectors.toList());
        } catch (Exception e) {
            // 인덱스/타입 이슈 대비 — 정렬 없이라도 반환
            System.err.println("[PolarLetterService] fallback without orderBy: " + e.getMessage());
            Query q = col();
            if (author != null && !author.isBlank()) q = q.whereEqualTo("author", author);
            QuerySnapshot qs = q.limit(n).get().get();
            return qs.getDocuments().stream().map(this::toRes).collect(Collectors.toList());
        }
    }

    /* ---------- Delete ---------- */
    public boolean delete(String id) throws Exception {
        var wr = col().document(id).delete().get();
        return wr.getUpdateTime() != null;
    }

    private PolarLetterRes toRes(DocumentSnapshot d) {
        return new PolarLetterRes(
                d.getId(),
                d.getString("author"),
                d.getString("title"),
                d.getString("content"),
                d.getString("url"),
                d.getString("thumbnail"),
                d.getString("create_time")
        );
    }
}
