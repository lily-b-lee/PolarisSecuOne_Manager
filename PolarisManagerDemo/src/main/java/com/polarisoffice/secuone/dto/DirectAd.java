package com.polarisoffice.secuone.dto;

import java.util.List;
import java.util.Map;

import com.google.cloud.Timestamp;

public class DirectAd {
    private String id;

    // 핵심 메타 (스크린샷 반영)
    private AdType adType;                 // 예: BOTTOM
    private String advertiserName;         // 예: "InfoVine"
    private String backgroundColor;        // "#FFFFFF"
    private String imageUrl;               // 배너 이미지
    private String targetUrl;              // 클릭 랜딩

    // 운영/타게팅(선택)
    private AdStatus status;               // 기본 ACTIVE
    private List<String> locales;          // ["ko-KR","en-US"]
    private String minAppVersion;          // "3.1.0"
    private String maxAppVersion;          // "3.9.99"

    // 시간
    private Timestamp publishedAt;         // 공개 시점
    private Timestamp startAt;             // 집행 시작
    private Timestamp endAt;               // 집행 종료
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // 집계
    private long viewCount;                // 노출수
    private long clickCount;               // 클릭수

    // 임의 메타
    private Map<String, Object> meta;

    // getters/setters...
    public enum AdType {
	    TOP, BOTTOM, INTERSTITIAL, FLOATING_FAB, INLINE, BANNER, EVENT, EVENT_FAB
	}

    public enum AdStatus {
	    DRAFT, ACTIVE, PAUSED, ARCHIVED
	}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AdType getAdType() {
        return adType;
    }

    public void setAdType(AdType adType) {
        this.adType = adType;
    }

    public String getAdvertiserName() {
        return advertiserName;
    }

    public void setAdvertiserName(String advertiserName) {
        this.advertiserName = advertiserName;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public AdStatus getStatus() {
        return status;
    }

    public void setStatus(AdStatus status) {
        this.status = status;
    }

    public List<String> getLocales() {
        return locales;
    }

    public void setLocales(List<String> locales) {
        this.locales = locales;
    }

    public String getMinAppVersion() {
        return minAppVersion;
    }

    public void setMinAppVersion(String minAppVersion) {
        this.minAppVersion = minAppVersion;
    }

    public String getMaxAppVersion() {
        return maxAppVersion;
    }

    public void setMaxAppVersion(String maxAppVersion) {
        this.maxAppVersion = maxAppVersion;
    }

    public Timestamp getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Timestamp publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Timestamp getStartAt() {
        return startAt;
    }

    public void setStartAt(Timestamp startAt) {
        this.startAt = startAt;
    }

    public Timestamp getEndAt() {
        return endAt;
    }

    public void setEndAt(Timestamp endAt) {
        this.endAt = endAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    public long getClickCount() {
        return clickCount;
    }

    public void setClickCount(long clickCount) {
        this.clickCount = clickCount;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }
    
    
    

}
