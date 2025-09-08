package com.polarisoffice.secuone.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 고객사 DTO (PK=code, RS%=rsPercent, CPI 금액=cpiValue)
 */
public class Customers {

  // ----------------------
  // 생성
  // ----------------------
  public static class CreateReq {
    public CreateReq() {}

    /** 코드: 소문자/숫자/-_ , 1~64자 */
    @NotBlank @Pattern(regexp="^[a-z0-9_-]{1,64}$")
    private String code;

    @NotBlank
    private String name;

    /** 연동방식: 예) API/FILE/MANUAL 등 */
    private String integrationType;

    /** RS(%) → 0.00 ~ 100.00 */
    @DecimalMin("0.00") @DecimalMax("100.00")
    private BigDecimal rsPercent;

    /** CPI(금액) → 0 이상 */
    @DecimalMin("0.00")
    private BigDecimal cpiValue;

    private String note;

    // getters/setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIntegrationType() { return integrationType; }
    public void setIntegrationType(String integrationType) { this.integrationType = integrationType; }
    public BigDecimal getRsPercent() { return rsPercent; }
    public void setRsPercent(BigDecimal rsPercent) { this.rsPercent = rsPercent; }
    public BigDecimal getCpiValue() { return cpiValue; }
    public void setCpiValue(BigDecimal cpiValue) { this.cpiValue = cpiValue; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
  }

  // ----------------------
  // 수정(부분 업데이트)
  // ----------------------
  public static class UpdateReq {
    public UpdateReq() {}

    private String name;
    private String integrationType;

    /** RS(%) → 0.00 ~ 100.00 */
    @DecimalMin("0.00") @DecimalMax("100.00")
    private BigDecimal rsPercent;

    /** CPI(금액) → 0 이상 */
    @DecimalMin("0.00")
    private BigDecimal cpiValue;

    private String note;

    // getters/setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIntegrationType() { return integrationType; }
    public void setIntegrationType(String integrationType) { this.integrationType = integrationType; }
    public BigDecimal getRsPercent() { return rsPercent; }
    public void setRsPercent(BigDecimal rsPercent) { this.rsPercent = rsPercent; }
    public BigDecimal getCpiValue() { return cpiValue; }
    public void setCpiValue(BigDecimal cpiValue) { this.cpiValue = cpiValue; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
  }

  // ----------------------
  // 응답
  // ----------------------
  public static class Res {
    public Res() {}

    public Res(String code, String name, String integrationType,
               BigDecimal rsPercent, BigDecimal cpiValue,
               String note, Instant createdAt, Instant updatedAt) {
      this.code = code;
      this.name = name;
      this.integrationType = integrationType;
      this.rsPercent = rsPercent;
      this.cpiValue = cpiValue;
      this.note = note;
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
    }

    private String code;
    private String name;
    private String integrationType;
    private BigDecimal rsPercent;
    private BigDecimal cpiValue;
    private String note;
    private Instant createdAt;
    private Instant updatedAt;

    // getters
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getIntegrationType() { return integrationType; }
    public BigDecimal getRsPercent() { return rsPercent; }
    public BigDecimal getCpiValue() { return cpiValue; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
  }
}
