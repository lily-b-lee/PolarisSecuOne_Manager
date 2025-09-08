package com.polarisoffice.secuone.repository;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.polarisoffice.secuone.domain.NoticeCategory;
import com.polarisoffice.secuone.dto.NoticeCreateReq;
import com.polarisoffice.secuone.dto.NoticeRes;
import com.polarisoffice.secuone.dto.NoticeUpdateReq;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class NoticeRepository {

  private static final String COLLECTION = "polarNotice";
  private static final DateTimeFormatter DOT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

  private CollectionReference col() {
    Firestore db = FirestoreClient.getFirestore();
    return db.collection(COLLECTION);
  }
  private String todayDot() { return LocalDate.now().format(DOT); }

  public String create(NoticeCreateReq req) throws Exception {
    Map<String,Object> data = new HashMap<>();
    if (req.author()!=null && !req.author().isBlank()) data.put("author", req.author().trim());
    if (req.category()!=null) data.put("category", req.category().name());
    data.put("title", req.title().trim());
    if (req.content()!=null && !req.content().isBlank()) data.put("content", req.content().trim());
    if (req.imageURL()!=null && !req.imageURL().isBlank()) data.put("imageURL", req.imageURL().trim());
    data.put("date", (req.date()==null || req.date().isBlank()) ? todayDot() : req.date().trim());
    data.put("updatedAt", OffsetDateTime.now().toString());

    DocumentReference doc = col().document();
    doc.set(data).get();
    return doc.getId();
  }

  public boolean update(String id, NoticeUpdateReq req) throws Exception {
    Map<String,Object> patch = new HashMap<>();
    if (req.author()!=null)   patch.put("author",   req.author().trim());
    if (req.category()!=null) patch.put("category", req.category().name());
    if (req.title()!=null)    patch.put("title",    req.title().trim());
    if (req.content()!=null)  patch.put("content",  req.content().trim());
    if (req.date()!=null)     patch.put("date",     req.date().trim());
    if (req.imageURL()!=null) patch.put("imageURL", req.imageURL().trim());
    if (patch.isEmpty()) return false;

    patch.put("updatedAt", OffsetDateTime.now().toString());
    WriteResult wr = col().document(id).update(patch).get();
    return wr.getUpdateTime()!=null;
  }

  public NoticeRes get(String id) throws Exception {
    DocumentSnapshot d = col().document(id).get().get();
    return d.exists() ? toRes(d) : null;
  }

  public List<NoticeRes> listLatest(Integer limit, NoticeCategory category) throws Exception {
    int n = Math.max(1, Math.min(limit==null ? 20 : limit, 100));
    Query q = col();
    if (category!=null) q = q.whereEqualTo("category", category.name());

    try {
      QuerySnapshot qs = q.orderBy("date", Query.Direction.DESCENDING)
                          .limit(n)
                          .get().get();
      return qs.getDocuments().stream().map(this::toRes).collect(Collectors.toList());
    } catch (Exception e) {
      // 인덱스/권한 문제시 넉넉히 가져와 메모리 정렬 후 상위 n개
      QuerySnapshot qs = q.limit(n*5).get().get();
      List<NoticeRes> list = qs.getDocuments().stream().map(this::toRes).collect(Collectors.toList());
      list.sort(Comparator.comparing(NoticeRes::date, Comparator.nullsLast(String::compareTo)).reversed());
      return list.stream().limit(n).collect(Collectors.toList());
    }
  }

  public boolean delete(String id) throws Exception {
    WriteResult wr = col().document(id).delete().get();
    return wr.getUpdateTime()!=null;
  }

  private NoticeRes toRes(DocumentSnapshot d){
    NoticeCategory cat = null;
    String raw = d.getString("category");
    if (raw!=null) { try { cat = NoticeCategory.from(raw); } catch (Exception ignore) {} }
    return new NoticeRes(
      d.getId(),
      d.getString("author"),
      cat,
      d.getString("title"),
      d.getString("content"),
      d.getString("date"),
      d.getString("imageURL")
    );
  }
}
