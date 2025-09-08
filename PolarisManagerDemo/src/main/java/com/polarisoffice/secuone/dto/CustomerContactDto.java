package com.polarisoffice.secuone.dto;

import lombok.*;

@Getter @Setter
@AllArgsConstructor @Builder
public class CustomerContactDto {
    public static class CreateReq {
      public String customerCode;   // 필수
      public String name;           // 필수
      public String email;          // 선택(없으면 초대 불가)
      public String phone;          // 선택
      public String note;           // 선택
      public Boolean sendInvite;    // 선택 (true면 메일 발송)
      public String getCustomerCode() {
          return customerCode;
      }
      public void setCustomerCode(String customerCode) {
          this.customerCode = customerCode;
      }
      public String getName() {
          return name;
      }
      public void setName(String name) {
          this.name = name;
      }
      public String getEmail() {
          return email;
      }
      public void setEmail(String email) {
          this.email = email;
      }
      public String getPhone() {
          return phone;
      }
      public void setPhone(String phone) {
          this.phone = phone;
      }
      public String getNote() {
          return note;
      }
      public void setNote(String note) {
          this.note = note;
      }
      public Boolean getSendInvite() {
          return sendInvite;
      }
      public void setSendInvite(Boolean sendInvite) {
          this.sendInvite = sendInvite;
      }
    }
    public static class Res {
      public Long id;
      public String customerCode;
      public String name;
      public String email;
      public String phone;
      public String note;
      public Res() {}
      public Res(Long id, String code, String name, String email, String phone, String note) {
        this.id=id; this.customerCode=code; this.name=name; this.email=email; this.phone=phone; this.note=note;
      }
    }
  }