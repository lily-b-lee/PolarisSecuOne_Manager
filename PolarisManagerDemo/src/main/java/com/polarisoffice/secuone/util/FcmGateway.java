package com.polarisoffice.secuone.util;

import com.google.firebase.messaging.*;
import com.polarisoffice.secuone.dto.PushRequest;

import org.springframework.stereotype.Component;
import java.util.List;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * 간단한 FCM 게이트웨이.
 * - 토픽/토큰 발송
 * - Android 채널 ID 지정(알림 채널 매핑)
 * - APNs 기본 사운드/배지
 * - 데이터 페이로드 전달
 * - dryRun(테스트) 지원: FCM 서버까지는 전송하되 실제 디바이스로는 미전달
 */
@Component
public class FcmGateway {

    /** 기본 TTL: 1시간 */
    private static final long DEFAULT_TTL_MILLIS = Duration.ofHours(1).toMillis();

    /**
     * 토픽으로 발송
     */
    public String sendToTopic(
            String topic,
            String title,
            String body,
            String androidChannelId,
            Map<String, String> data,
            boolean dryRun
    ) throws FirebaseMessagingException {
        Message msg = baseMessageBuilder(title, body, androidChannelId, data, AndroidConfig.Priority.HIGH)
                .setTopic(topic)
                .build();
        return FirebaseMessaging.getInstance().send(msg, dryRun);
    }

    /**
     * 단일 디바이스 토큰으로 발송
     */
    public String sendToToken(
            String token,
            String title,
            String body,
            String androidChannelId,
            Map<String, String> data,
            boolean dryRun
    ) throws FirebaseMessagingException {
        Message msg = baseMessageBuilder(title, body, androidChannelId, data, AndroidConfig.Priority.HIGH)
                .setToken(token)
                .build();
        return FirebaseMessaging.getInstance().send(msg, dryRun);
    }

    /**
     * (선택) 클릭 액션을 포함해 발송하고 싶을 때 사용하는 오버로드.
     * Android 에서는 setClickAction, iOS 에서는 "click_action" 데이터를 활용하세요.
     */
    public String sendToTopic(
            String topic,
            String title,
            String body,
            String androidChannelId,
            String clickAction,
            Map<String, String> data,
            boolean dryRun
    ) throws FirebaseMessagingException {
        Message msg = baseMessageBuilder(title, body, androidChannelId, withClickAction(data, clickAction), AndroidConfig.Priority.HIGH)
                .setTopic(topic)
                .build();
        return FirebaseMessaging.getInstance().send(msg, dryRun);
    }

    // ===== 내부 유틸 =====

    private static Map<String, String> withClickAction(Map<String, String> data, String clickAction) {
        if (clickAction == null || clickAction.isBlank()) return data;
        if (data == null || data.isEmpty()) {
            return Collections.singletonMap("click_action", clickAction);
        }
        // 복사 방지: 필요한 경우 새 맵으로
        new java.util.HashMap<>(data).put("click_action", clickAction);
        Map<String,String> m = new java.util.HashMap<>(data);
        m.put("click_action", clickAction);
        return m;
    }

    /**
     * 공통 메시지 빌더
     */
    private static Message.Builder baseMessageBuilder(
            String title,
            String body,
            String androidChannelId,
            Map<String, String> data,
            AndroidConfig.Priority priority
    ) {
        Notification notif = Notification.builder()
                .setTitle(nullToEmpty(title))
                .setBody(nullToEmpty(body))
                .build();

        AndroidNotification.Builder androidNotif = AndroidNotification.builder()
                .setChannelId(nullToEmpty(androidChannelId))
                .setSound("default"); // 채널 사운드 설정을 따르지만 기본값 지정

        AndroidConfig android = AndroidConfig.builder()
                .setTtl(DEFAULT_TTL_MILLIS)
                .setPriority(priority != null ? priority : AndroidConfig.Priority.HIGH)
                .setNotification(androidNotif.build())
                .build();

        // iOS
        Aps aps = Aps.builder()
                .setSound("default")
                .setBadge(1)
                .build();
        ApnsConfig apns = ApnsConfig.builder()
                .setAps(aps)
                .build();

        Message.Builder mb = Message.builder()
                .setNotification(notif)
                .setAndroidConfig(android)
                .setApnsConfig(apns);

        if (data != null && !data.isEmpty()) {
            mb.putAllData(data);
        }
        return mb;
    }
    public List<String> send(PushRequest req, String fallbackChannelId) throws FirebaseMessagingException {
        final boolean dryRun = false; // req.isTest() 같은 걸 쓰면 더 좋음
        final String channelId = (req.getData()!=null && req.getData().containsKey("androidChannelId"))
                ? req.getData().get("androidChannelId")
                : fallbackChannelId;

        // 1) 멀티캐스트
        if (req.getTokens()!=null && !req.getTokens().isEmpty()) {
            MulticastMessage.Builder mb = MulticastMessage.builder()
                    .addAllTokens(req.getTokens())
                    .putAllData(req.getData()==null? Map.of() : req.getData())
                    .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(req.isHighPriority()? AndroidConfig.Priority.HIGH : AndroidConfig.Priority.NORMAL)
                        .setNotification(AndroidNotification.builder()
                            .setChannelId(channelId)
                            .setSound("default")
                            .build())
                        .build())
                    .setNotification(Notification.builder()
                        .setTitle(nullToEmpty(req.getTitle()))
                        .setBody(nullToEmpty(req.getBody()))
                        .build());

            BatchResponse br = FirebaseMessaging.getInstance().sendEachForMulticast(mb.build(), dryRun);
            // 개별 messageId가 섞여 있어서 간단히 성공 수만 반환하고 싶다면:
            return java.util.stream.IntStream.range(0, br.getResponses().size())
                    .filter(i -> br.getResponses().get(i).isSuccessful())
                    .mapToObj(i -> br.getResponses().get(i).getMessageId())
                    .toList();
        }

        // 2) 단건 토큰
        if (req.getToken()!=null && !req.getToken().isBlank()) {
            return java.util.List.of(
                sendToToken(req.getToken(), req.getTitle(), req.getBody(), channelId, req.getData(), dryRun)
            );
        }

        // 3) 토픽
        if (req.getTopic()!=null && !req.getTopic().isBlank()) {
            return java.util.List.of(
                sendToTopic(req.getTopic(), req.getTitle(), req.getBody(), channelId, req.getData(), dryRun)
            );
        }

        throw new IllegalArgumentException("PushRequest: token/tokens/topic 중 하나는 반드시 있어야 합니다.");
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
