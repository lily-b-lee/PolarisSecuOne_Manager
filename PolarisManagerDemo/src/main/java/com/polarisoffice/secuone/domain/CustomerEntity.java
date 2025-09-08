// src/main/java/com/polarisoffice/secuone/domain/CustomerEntity.java
package com.polarisoffice.secuone.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "customers", indexes = {
  @Index(name = "uk_customers_domain", columnList = "domain", unique = true)
})
public class CustomerEntity {
  @Id
  @Column(nullable = false, length = 50)
  private String code; // ✅ PK = String

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 190, unique = true)
  private String domain;

  @Column(length = 64)
  private String integrationType;

  @Column(precision = 10, scale = 2)
  private BigDecimal rsPercent;

  @Column(precision = 12, scale = 2)
  private BigDecimal cpiValue;

  @Column(length = 2000)
  private String note;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @PrePersist
  public void prePersist() { /* code/domain normalize; set timestamps */ }

  @PreUpdate
  public void preUpdate() { /* code/domain normalize; set updatedAt */ }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getIntegrationType() {
    return integrationType;
  }

  public void setIntegrationType(String integrationType) {
    this.integrationType = integrationType;
  }

  public BigDecimal getRsPercent() {
    return rsPercent;
  }

  public void setRsPercent(BigDecimal rsPercent) {
    this.rsPercent = rsPercent;
  }

  public BigDecimal getCpiValue() {
    return cpiValue;
  }

  public void setCpiValue(BigDecimal cpiValue) {
    this.cpiValue = cpiValue;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  // getters/setters ...
  
  
}

