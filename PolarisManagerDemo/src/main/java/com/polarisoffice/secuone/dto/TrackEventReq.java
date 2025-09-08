package com.polarisoffice.secuone.dto;

import jakarta.validation.constraints.NotBlank;

/** 노출/클릭 공통 이벤트 */
public class TrackEventReq {
    @NotBlank public String placement;   // 화면/위치 키(e.g. "Home:BottomBanner")
    public String appVersion;            // "4.2.1"
    public String deviceModel;           // "SM-S911N"
    public String osVersion;             // "Android 14"
    public String locale;                // "ko-KR"
    public String sessionId;             // 클라 세션 식별용(선택)
    public String clientId;              // 익명 사용자 키(선택, 해시)
    public double latitude;              // 선택
    public double longitude;             // 선택
}