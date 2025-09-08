package com.polarisoffice.secuone.service;

import com.polarisoffice.secuone.domain.NoticeCategory;
import com.polarisoffice.secuone.dto.NoticeCreateReq;
import com.polarisoffice.secuone.dto.NoticeRes;
import com.polarisoffice.secuone.dto.NoticeUpdateReq;
import com.polarisoffice.secuone.repository.NoticeRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NoticeService {

  private final NoticeRepository repo;
  private final NoticePushService push;

  public NoticeService(NoticeRepository repo, NoticePushService push) {
    this.repo = repo;
    this.push = push;
  }

  /** 저장만 */
  public String create(NoticeCreateReq req) throws Exception {
    return repo.create(req);
  }

  /** 저장 + 카테고리 기반 푸시 */
  public String createAndPush(NoticeCreateReq req, boolean test) throws Exception {
	    String id = repo.create(req);
	    // 본문 80자 트림은 NoticePushService 내부에서 처리
	    push.notifyNotice(req, id, test);
	    return id;
  }

  public boolean update(String id, NoticeUpdateReq req) throws Exception { return repo.update(id, req); }
  public NoticeRes get(String id) throws Exception { return repo.get(id); }
  public List<NoticeRes> listLatest(Integer limit, NoticeCategory category) throws Exception {
    return repo.listLatest(limit, category);
  }
  public boolean delete(String id) throws Exception { return repo.delete(id); }
}
