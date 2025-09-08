package com.polarisoffice.secuone.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.polarisoffice.secuone.domain.NoticeCategory;
import com.polarisoffice.secuone.dto.NoticeCreateReq;
import com.polarisoffice.secuone.dto.NoticeRes;
import com.polarisoffice.secuone.util.FcmGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class NoticePushService {

    private static final Logger log = LoggerFactory.getLogger(NoticePushService.class);

    private final FcmGateway fcm; // 실제 FCM 발송 게이트웨이

    // 채널 ID 상수 (요청대로)
    public static final String EVENT_CHANNEL_ID     = "EVENT_CHANNEL_ID";
    public static final String EMERGENCY_CHANNEL_ID = "EMERGENCY_CHANNEL_ID";
    public static final String NOTICE_CHANNEL_ID    = "NOTICE_CHANNEL_ID"; // 가이드/업데이트

    public NoticePushService(FcmGateway fcm) {
        this.fcm = fcm;
    }

    /** 생성 요청 DTO로 알림 */
    public void notifyNotice(NoticeCreateReq req, String id, boolean test) {
        send(req.title(), req.content(), req.category(), id, test);
    }

    /** 조회/응답 DTO로 알림 (id를 따로 받는 기존 시그니처 유지) */
    public void notifyNotice(NoticeRes res, String id, boolean test) {
        send(res.title(), res.content(), res.category(), id, test);
    }

    private void send(String title, String content, NoticeCategory category, String id, boolean test) {
        final String channel = resolveChannel(category);
        final String topic   = test ? channel + "_TEST" : channel;

        // 앱에서 활용할 데이터 페이로드
        Map<String, String> data = new HashMap<>();
        data.put("type", "notice");
        if (id != null)          data.put("id", id);
        if (category != null)    data.put("category", category.name());
        // 안드로이드 채널 고정 연결(클라이언트에서 채널 스위칭할 때 사용)
        data.put("androidChannelId", channel);

        String t = title  == null ? "" : title;
        String b = content== null ? "" : content;

        try {
            // 게이트웨이의 토픽 전송 API 사용 (dryRun=test)
            fcm.sendToTopic(topic, t, b, channel, data, test);
            log.info("[NoticePushService] pushed topic={}, title={}, id={}, test={}", topic, t, id, test);
        } catch (FirebaseMessagingException e) {
            // 알림 실패가 저장 자체를 막지 않도록 예외는 로깅만
            log.warn("[NoticePushService] FCM push failed: topic={}, id={}, cause={}", topic, id, e.getMessage(), e);
        }
    }

    /** 카테고리 → 채널 라우팅 */
    private String resolveChannel(NoticeCategory category) {
        if (category == null) return NOTICE_CHANNEL_ID;
        return switch (category) {
            case EVENT -> EVENT_CHANNEL_ID;
            case EMERGENCY -> EMERGENCY_CHANNEL_ID;
            case SERVICE_GUIDE, UPDATE -> NOTICE_CHANNEL_ID;
        };
    }
}
