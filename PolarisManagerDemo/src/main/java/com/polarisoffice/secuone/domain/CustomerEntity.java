// src/main/java/com/polarisoffice/secuone/domain/CustomerEntity.java
package com.polarisoffice.secuone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "customers")
@Getter @Setter
public class CustomerEntity extends BaseTimeEntity {

  @Id
  @Column(name = "code", length = 64, nullable = false, updatable = false)
  private String code;

  @Column(name = "name", length = 255, nullable = false)
  private String name;

  @Column(name = "domain", length = 255)
  private String domain;

  @Column(name = "integration_type", length = 64)
  private String integrationType;   // 예: GENERIC

  @Column(name = "rs_percent", precision = 10, scale = 2)
  private BigDecimal rsPercent;

  @Column(name = "cpi_value", precision = 10, scale = 2)
  private BigDecimal cpiValue;

  @Column(name = "note", length = 1000)
  private String note;

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
  
  
}
