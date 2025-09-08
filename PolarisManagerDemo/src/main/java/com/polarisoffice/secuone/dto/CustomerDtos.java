package com.polarisoffice.secuone.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/* 고객사 */
public class CustomerDtos {

  /* 생성/수정 요청 */
  public static class CustomerReq {
    public String code;                // PK (생성 시 필수)
    public String name;                // 필수
    public String integrationType;     // 예: API/FILE/MANUAL
    public BigDecimal rsPercent;       // RS(%)
    public BigDecimal cpiValue;        // CPI(금액)
    public String note;
  }

  /* 단건 응답 */
  public static class CustomerRes {
    public String code;
    public String name;
    public String integrationType;
    public BigDecimal rsPercent;
    public BigDecimal cpiValue;
    public String note;
    public Instant createdAt;
    public Instant updatedAt;
  }

  /* 목록 아이템 */
  public static class CustomerListItem {
    public String code;
    public String name;
    public String integrationType;
    public BigDecimal rsPercent;
    public BigDecimal cpiValue;
  }
}
