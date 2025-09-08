package com.polarisoffice.secuone.dto;

/** 고객사 담당자 DTO 모음 */
public class ContactDtos {

    /** 요청 DTO (업서트/생성/수정 공용) */
    public static class ContactReq {
        /** 기존 연락처 갱신을 위한 ID (없으면 신규 생성) */
        public Long id;

        /** 고객사 코드(필수) */
        public String customerCode;

        /** 담당자 이름 */
        public String name;

        /** 담당자 이메일(읽기전용 운영이라면 프런트에서 수정 안 함) */
        public String email;

        /** 담당자 휴대전화 */
        public String phone;

        /** 비고/메모 */
        public String note;

        /** 대표 담당자 여부(옵션) */
        public Boolean isPrimary;

        /**
         * 연락처 저장 시 계정(CustomerUser)도 같이 만들지 여부(옵션)
         * 서비스에서 필요 없으면 무시해도 됨.
         */
        public Boolean createAccount;

        /** (기존 필드 유지) 초대메일 발송 여부 등에서 사용 */
        public Boolean sendInvite;

        public ContactReq() {}

        // ---- getters / setters ----
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getCustomerCode() { return customerCode; }
        public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public Boolean getIsPrimary() { return isPrimary; }
        public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
        // 호환용
        public Boolean isPrimary() { return isPrimary; }

        public Boolean getCreateAccount() { return createAccount; }
        public void setCreateAccount(Boolean createAccount) { this.createAccount = createAccount; }

        public Boolean getSendInvite() { return sendInvite; }
        public void setSendInvite(Boolean sendInvite) { this.sendInvite = sendInvite; }
    }

    /** 응답 DTO */
    public static class ContactRes {
        public Long id;
        public String customerCode;
        public String name;
        public String email;
        public String phone;
        public String note;

        /** 대표 담당자 여부(옵션) */
        public Boolean isPrimary;

        // 선택적 반환(계정 자동 생성 시)
        public String generatedUsername;
        public String tempPassword;

        public ContactRes() {}

        // ---- getters / setters ----
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getCustomerCode() { return customerCode; }
        public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public Boolean getIsPrimary() { return isPrimary; }
        public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
        // 호환용
        public Boolean isPrimary() { return isPrimary; }

        public String getGeneratedUsername() { return generatedUsername; }
        public void setGeneratedUsername(String generatedUsername) { this.generatedUsername = generatedUsername; }

        public String getTempPassword() { return tempPassword; }
        public void setTempPassword(String tempPassword) { this.tempPassword = tempPassword; }
    }
}
