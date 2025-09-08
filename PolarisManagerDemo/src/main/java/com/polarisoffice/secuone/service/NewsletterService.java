package com.polarisoffice.secuone.service;

import org.springframework.stereotype.Service;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.polarisoffice.secuone.dto.NewsletterCreateReq;
import com.polarisoffice.secuone.dto.NewsletterRes;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NewsletterService {
	
	private static final String COLLECTION = "secuNews";

	    private CollectionReference col() {
	        Firestore db = FirestoreClient.getFirestore();
	        return db.collection(COLLECTION);
	    }

	    /** 저장(생성) */
	    public String create(NewsletterCreateReq req) throws Exception {
	        String date = (req.date() == null || req.date().isBlank())
	                ? LocalDate.now().toString()  // YYYY-MM-DD
	                : req.date().trim();

	        Map<String, Object> data = new HashMap<>();
	        if (req.category() != null && !req.category().isBlank()) data.put("category", req.category().trim());
	        data.put("date", date);
	        data.put("title", req.title().trim());
	        if (req.url() != null && !req.url().isBlank()) data.put("url", req.url().trim());
	        if (req.thumbnail() != null && !req.thumbnail().isBlank()) data.put("thumbnail", req.thumbnail().trim());

	        // 서버측 업데이트 시간(문자열)
	        data.put("updatedAt", java.time.OffsetDateTime.now().toString());

	        DocumentReference doc = col().document(); // auto-ID
	        doc.set(data).get();                      // 동기 저장
	        return doc.getId();
	    }

	    public void update(String id, String title, String content) throws Exception {
	        Map<String, Object> doc = Map.of(
	                "title", title,
	                "content", content,
	                "updatedAt", Instant.now().toString()
	        );
	        col().document(id).set(doc, SetOptions.merge()).get();
	    }

//	    public NewsletterRes get(String id) throws Exception {
//	        DocumentSnapshot snap = col().document(id).get().get();
//	        if (!snap.exists()) return null;
//	        return new NewsletterRes(
//	                snap.getId(),
//	                snap.getString("title"),
//	                snap.getString("content"),
//	                snap.getString("updatedAt")
//	        );
//	    }
//
//	    public List<NewsletterRes> listLatest(int limit) throws Exception {
//	        List<NewsletterRes> out = new ArrayList<>();
//	        QuerySnapshot qs = col()
//	                .orderBy("updatedAt", Query.Direction.DESCENDING)
//	                .limit(limit)
//	                .get().get();
//	        for (DocumentSnapshot d : qs.getDocuments()) {
//	            out.add(new NewsletterRes(
//	                    d.getId(),
//	                    d.getString("title"),
//	                    d.getString("content"),
//	                    d.getString("updatedAt")
//	            ));
//	        }
//	        return out;
//	    }
	    
	    public NewsletterRes get(String id) throws Exception {
	        DocumentSnapshot d = col().document(id).get().get();
	        if (!d.exists()) return null;
	        return toRes(d);
	    }

	    public List<NewsletterRes> listLatest(int limit) throws Exception {
	        List<NewsletterRes> out = new ArrayList<>();
	        // date가 "YYYY-MM-DD"라서 문자열 정렬로 최신순 동작합니다.
	        QuerySnapshot qs = col()
	                .orderBy("date", Query.Direction.DESCENDING)
	                .limit(Math.max(1, Math.min(limit, 100)))
	                .get().get();
	        for (DocumentSnapshot d : qs.getDocuments()) out.add(toRes(d));
	        return out;
	    }

	    private NewsletterRes toRes(DocumentSnapshot d) {
	        return new NewsletterRes(
	                d.getId(),
	                d.getString("category"),
	                d.getString("date"),
	                d.getString("thumbnail"),
	                d.getString("title"),
	                d.getString("url")
	        );
	    }
}
