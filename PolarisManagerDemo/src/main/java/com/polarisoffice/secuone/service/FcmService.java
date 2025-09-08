package com.polarisoffice.secuone.service;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.*;
import com.polarisoffice.secuone.dto.PushRequest;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FcmService {


	public String sendToToken(PushRequest req) throws FirebaseMessagingException {
	    Message message = baseMessageBuilder(req)
	            .setToken(req.getToken())   // ✅ 여기!
	            .build();

	    return FirebaseMessaging.getInstance().send(message); // messageId 반환
	}

    public BatchResponse sendMulticast(PushRequest req) throws FirebaseMessagingException {
        List<String> tokens = Optional.ofNullable(req.getTokens()).orElse(Collections.emptyList());
        if (tokens.isEmpty()) throw new IllegalArgumentException("tokens is empty");

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(req.getTitle())
                        .setBody(req.getBody())
                        .build())
                .putAllData(Optional.ofNullable(req.getData()).orElse(Collections.emptyMap()))
                .setAndroidConfig(androidConfig(req))
                .setApnsConfig(apnsConfig(req))
                .addAllTokens(tokens)
                .build();

        return FirebaseMessaging.getInstance().sendMulticast(message);
    }

    public String sendToTopic(PushRequest req) throws FirebaseMessagingException {
        if (req.getTopic() == null || req.getTopic().isBlank())
            throw new IllegalArgumentException("topic is empty");

        Message message = baseMessageBuilder(req)
                .setTopic(req.getTopic())
                .build();

        return FirebaseMessaging.getInstance().send(message);
    }

    // 공통 빌더
    private Message.Builder baseMessageBuilder(PushRequest req) {
        return Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(req.getTitle())
                        .setBody(req.getBody())
                        .build())
                .putAllData(Optional.ofNullable(req.getData()).orElse(Collections.emptyMap()))
                .setAndroidConfig(androidConfig(req))
                .setApnsConfig(apnsConfig(req));
    }

    private AndroidConfig androidConfig(PushRequest req) {
        AndroidConfig.Priority priority = req.isHighPriority()
                ? AndroidConfig.Priority.HIGH
                : AndroidConfig.Priority.NORMAL;

        return AndroidConfig.builder()
                .setPriority(priority)
                .setTtl(Duration.ofHours(1).toMillis()) // 1시간 TTL 예시
                .setNotification(AndroidNotification.builder()
                        .setSound("default")  // 필요시 커스텀 사운드
                        .build())
                .build();
    }

    private ApnsConfig apnsConfig(PushRequest req) {
        Aps aps = Aps.builder()
                .setContentAvailable(true)
                .setSound("default")
                .build();

        return ApnsConfig.builder()
                .putHeader("apns-priority", req.isHighPriority() ? "10" : "5")
                .setAps(aps)
                .build();
    }

    // 실패 토큰 정리 보조 메서드 (멀티캐스트 전용)
    public List<String> extractInvalidTokens(BatchResponse batchResponse, List<String> tokens) {
        List<String> invalid = new ArrayList<>();
        for (int i = 0; i < batchResponse.getResponses().size(); i++) {
            SendResponse r = batchResponse.getResponses().get(i);
            if (!r.isSuccessful()) {
                Exception e = r.getException();
                if (e instanceof FirebaseMessagingException) {
                    FirebaseMessagingException fme = (FirebaseMessagingException) e;
                    String code = fme.getMessagingErrorCode() != null
                            ? fme.getMessagingErrorCode().name()
                            : "";
                    if ("UNREGISTERED".equals(code) || "INVALID_ARGUMENT".equals(code)) {
                        invalid.add(tokens.get(i));
                    }
                }
            }
        }
        return invalid;
    }
}
