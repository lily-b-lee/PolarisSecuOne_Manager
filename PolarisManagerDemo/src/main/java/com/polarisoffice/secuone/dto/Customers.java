package com.polarisoffice.secuone.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 고객사 DTO 묶음
 * - cpiRate: 원화 금액(정수, 예: 2000, 3000)
 * - rsRate : 퍼센트(0~100, 소수 2자리까지 허용 예: 3.5, 12.25)
 * - CreateReq: isPrimary 기본값 false
 * - UpdateReq: isPrimary는 선택 변경(Boolean) — null이면 미변경 의미
 */
public final class Customers {

    private Customers() { }

    /* ========= 요청 ========= */

    public static class CreateReq {
        @NotBlank public String code;
        @NotBlank public String name;
        public String integrationType;   // 예: "API" / "SFTP" ...

        /** 원화 금액(정수). 예: 2000, 3000 */
        @NotNull
        @PositiveOrZero
        @Digits(integer = 12, fraction = 0)  // 원화: 소수 없음
        public BigDecimal cpiRate;

        /** 퍼센트(0~100), 소수 2자리까지 허용 */
        @NotNull
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        @Digits(integer = 5, fraction = 2)
        public BigDecimal rsRate;

        public String note;

        /** 신규 생성 기본값: false (DB is_primary NOT NULL 보호) */
        public boolean isPrimary = false;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getIntegrationType() { return integrationType; }
        public void setIntegrationType(String integrationType) { this.integrationType = integrationType; }

        public BigDecimal getCpiRate() { return cpiRate; }
        public void setCpiRate(BigDecimal cpiRate) { this.cpiRate = cpiRate; }

        public BigDecimal getRsRate() { return rsRate; }
        public void setRsRate(BigDecimal rsRate) { this.rsRate = rsRate; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        // boolean 표준 접근자
        public boolean isPrimary() { return isPrimary; }
        public void setPrimary(boolean primary) { isPrimary = primary; }

        // Jackson 호환 보조 접근자(선택)
        public boolean getIsPrimary() { return isPrimary; }
        public void setIsPrimary(boolean primary) { isPrimary = primary; }
    }

    public static class UpdateReq {
        public String name;
        public String integrationType;

        /** 원화 금액(정수). 예: 2000, 3000 */
        @PositiveOrZero
        @Digits(integer = 12, fraction = 0)
        public BigDecimal cpiRate;

        /** 퍼센트(0~100), 소수 2자리까지 허용 */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        @Digits(integer = 5, fraction = 2)
        public BigDecimal rsRate;

        public String note;

        /** null이면 미변경, true/false가 들어오면 변경 */
        public Boolean isPrimary;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getIntegrationType() { return integrationType; }
        public void setIntegrationType(String integrationType) { this.integrationType = integrationType; }

        public BigDecimal getCpiRate() { return cpiRate; }
        public void setCpiRate(BigDecimal cpiRate) { this.cpiRate = cpiRate; }

        public BigDecimal getRsRate() { return rsRate; }
        public void setRsRate(BigDecimal rsRate) { this.rsRate = rsRate; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public Boolean getIsPrimary() { return isPrimary; }
        public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    }

    /* ========= 응답 ========= */

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Res {
        public String code;
        public String name;
        public String integrationType;

        /** 퍼센트(0~100) */
        public BigDecimal rsRate;

        /** 원화 금액(정수) */
        public BigDecimal cpiRate;

        public String note;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;

        /** 응답에 노출되는 값 */
        public boolean isPrimary;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getIntegrationType() { return integrationType; }
        public void setIntegrationType(String integrationType) { this.integrationType = integrationType; }

        public BigDecimal getRsRate() { return rsRate; }
        public void setRsRate(BigDecimal rsRate) { this.rsRate = rsRate; }

        public BigDecimal getCpiRate() { return cpiRate; }
        public void setCpiRate(BigDecimal cpiRate) { this.cpiRate = cpiRate; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        public boolean isPrimary() { return isPrimary; }
        public void setPrimary(boolean primary) { isPrimary = primary; }

        // 보조 접근자
        public boolean getIsPrimary() { return isPrimary; }
        public void setIsPrimary(boolean primary) { isPrimary = primary; }

        // (A) 기존 컨트롤러 호환용 8개 생성자
        public Res(
                String code,
                String name,
                String integrationType,
                BigDecimal rsRate,
                BigDecimal cpiRate,
                String note,
                LocalDateTime createdAt,
                LocalDateTime updatedAt
        ) {
            this(code, name, integrationType, rsRate, cpiRate, note, createdAt, updatedAt, false);
        }

        // (B) 신규 9개 생성자 (isPrimary까지 포함)
        public Res(
                String code,
                String name,
                String integrationType,
                BigDecimal rsRate,
                BigDecimal cpiRate,
                String note,
                LocalDateTime createdAt,
                LocalDateTime updatedAt,
                boolean isPrimary
        ) {
            this.code = code;
            this.name = name;
            this.integrationType = integrationType;
            this.rsRate = rsRate;
            this.cpiRate = cpiRate;
            this.note = note;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.isPrimary = isPrimary;
        }
    }
}
