package com.polarisoffice.secuone.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.polarisoffice.secuone.dto.PushRequest;
import com.polarisoffice.secuone.service.FcmService;
import com.google.firebase.messaging.BatchResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

record PushReq(@NotBlank String token, @NotBlank String title, @NotBlank String body) {}

@RestController
@RequestMapping("/api/push")
public class NotifyController {

    private final FcmService fcmService;

    public NotifyController(FcmService fcmService) {
        this.fcmService = fcmService;
    }

    // 단일 토큰 발송
    @PostMapping("/token")
    public ResponseEntity<?> sendToToken(@RequestBody PushRequest req) throws FirebaseMessagingException {
        String messageId = fcmService.sendToToken(req);
        return ResponseEntity.ok().body(new ApiResult("OK", messageId));
    }

    // 멀티캐스트 발송
    @PostMapping("/tokens")
    public ResponseEntity<?> sendToTokens(@RequestBody PushRequest req) throws FirebaseMessagingException {
        BatchResponse res = fcmService.sendMulticast(req);
        List<String> invalid = fcmService.extractInvalidTokens(res, req.getTokens());
        return ResponseEntity.ok().body(new BulkResult("OK", res.getSuccessCount(), res.getFailureCount(), invalid));
    }

    // 토픽 발송
    @PostMapping("/topic")
    public ResponseEntity<?> sendToTopic(@RequestBody PushRequest req) throws FirebaseMessagingException {
        String messageId = fcmService.sendToTopic(req);
        return ResponseEntity.ok().body(new ApiResult("OK", messageId));
    }

    // 간단 응답 모델
    record ApiResult(String status, String messageId) {}
    record BulkResult(String status, int success, int failure, List<String> invalidTokens) {}
}
