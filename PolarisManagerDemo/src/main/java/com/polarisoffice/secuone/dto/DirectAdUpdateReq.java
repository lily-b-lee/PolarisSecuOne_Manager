package com.polarisoffice.secuone.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.polarisoffice.secuone.dto.DirectAd.AdStatus;
import com.polarisoffice.secuone.dto.DirectAd.AdType;

public class DirectAdUpdateReq {
    private AdType adType;
    private String advertiserName;
    private String backgroundColor;
    private String imageUrl;
    private String targetUrl;
    private AdStatus status;
    private List<String> locales;
    private String minAppVersion;
    private String maxAppVersion;
    private Instant publishedAt;
    private Instant startAt;
    private Instant endAt;
    private Map<String, Object> meta;
    // getters/setters...
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
    public Instant getPublishedAt() {
        return publishedAt;
    }
    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
    public Instant getStartAt() {
        return startAt;
    }
    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }
    public Instant getEndAt() {
        return endAt;
    }
    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }
    public Map<String, Object> getMeta() {
        return meta;
    }
    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }
    
}
