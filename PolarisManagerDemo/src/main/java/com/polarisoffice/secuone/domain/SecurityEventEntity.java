// src/main/java/com/polarisoffice/secuone/domain/SecurityEventEntity.java
package com.polarisoffice.secuone.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "security_events", indexes = {
    @Index(name="ix_events_customer_code", columnList = "customerCode"),
    @Index(name="ix_events_created_at", columnList = "createdAt")
})
public class SecurityEventEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false, length=64)
  private String customerCode;

  @Column(length=128)
  private String deviceId;

  @Column(nullable=false, length=64)
  private String eventType;

  @Column(length=255)
  private String sourcePackage; // 연동앱 패키지

  @Column(length=255)
  private String sourceDomain;  // 유입 웹 도메인

  @Lob
  @Column(columnDefinition = "TEXT")
  private String payloadJson;   // 상세 데이터

  @Column(nullable=false)
  private Instant createdAt = Instant.now();

  // getters/setters...
  public Long getId(){ return id; }
  public String getCustomerCode(){ return customerCode; }
  public void setCustomerCode(String code){ this.customerCode = code; }
  public String getDeviceId(){ return deviceId; }
  public void setDeviceId(String deviceId){ this.deviceId = deviceId; }
  public String getEventType(){ return eventType; }
  public void setEventType(String eventType){ this.eventType = eventType; }
  public String getSourcePackage(){ return sourcePackage; }
  public void setSourcePackage(String sourcePackage){ this.sourcePackage = sourcePackage; }
  public String getSourceDomain(){ return sourceDomain; }
  public void setSourceDomain(String sourceDomain){ this.sourceDomain = sourceDomain; }
  public String getPayloadJson(){ return payloadJson; }
  public void setPayloadJson(String payloadJson){ this.payloadJson = payloadJson; }
  public Instant getCreatedAt(){ return createdAt; }
  public void setCreatedAt(Instant createdAt){ this.createdAt = createdAt; }
}
