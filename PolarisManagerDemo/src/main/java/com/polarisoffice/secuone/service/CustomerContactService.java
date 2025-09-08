// src/main/java/com/polarisoffice/secuone/service/CustomerContactService.java
package com.polarisoffice.secuone.service;

import com.polarisoffice.secuone.domain.CustomerContactEntity;
import com.polarisoffice.secuone.domain.CustomerEntity;
import com.polarisoffice.secuone.dto.ContactDtos;
import com.polarisoffice.secuone.dto.ContactDtos.ContactReq;
import com.polarisoffice.secuone.dto.ContactDtos.ContactRes;
import com.polarisoffice.secuone.repository.CustomerContactRepository;
import com.polarisoffice.secuone.repository.CustomerRepository;
import com.polarisoffice.secuone.repository.CustomerUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerContactService {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(CustomerContactService.class);

  private final CustomerRepository customers;          // 고객사
  private final CustomerContactRepository contacts;    // 담당자
  private final CustomerUserRepository users;          // (옵션) 계정
  private final PasswordEncoder encoder;               // (옵션) 비밀번호 인코더
  private final MailService mail;                      // (옵션) 메일

  public CustomerContactService(
      CustomerRepository customers,
      CustomerContactRepository contacts,
      CustomerUserRepository users,
      PasswordEncoder encoder,
      MailService mail
  ) {
    this.customers = customers;
    this.contacts = contacts;
    this.users = users;
    this.encoder = encoder;
    this.mail = mail;
  }

  /**
   * 담당자 upsert
   * - 우선 id로 찾고 없으면 (customerCode, email)로 찾음
   * - 그래도 없으면 새로 생성
   * - email은 읽기 전용 가정(키 역할)
   */
  @Transactional
  public ContactRes upsertAndMaybeCreateAccount(ContactReq req) {
    if (req == null) throw new IllegalArgumentException("요청이 비어 있습니다.");

    final Long   id           = req.getId();
    final String customerCode = nz(req.getCustomerCode());
    final String email        = nz(req.getEmail());

    if (customerCode == null || customerCode.isBlank()) {
      throw new IllegalArgumentException("customerCode 는 필수입니다.");
    }
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("email 은 필수입니다.");
    }

    // 1) id로 먼저 찾기
    CustomerContactEntity e = null;
    if (id != null) {
      e = contacts.findById(id).orElse(null);
    }

    // 2) 없으면 (customer_code, email) 로 찾기
    if (e == null) {
      e = contacts
          .findFirstByCustomer_CodeAndEmailIgnoreCase(customerCode, email)
          .orElse(null);
    }

    // 3) 그래도 없으면 새로 생성
 // 3) 그래도 없으면 새로 생성
    if (e == null) {
      CustomerEntity customer = customers.findByCode(customerCode)
          .orElseThrow(() ->
              new IllegalArgumentException("Invalid customerCode: " + customerCode));

      e = new CustomerContactEntity();
      e.setCustomer(customer);
      e.setEmail(email);
    }

    // 공통 필드 갱신
    e.setName(nz(req.getName()));
    e.setPhone(nz(req.getPhone()));
    e.setNote(req.getNote());
    if (req.getIsPrimary() != null) {
      e.setIsPrimary(req.getIsPrimary());
    }

    e = contacts.save(e);

    // 응답 매핑
    ContactRes res = new ContactRes();
    res.setId(e.getId());
    res.setCustomerCode(e.getCustomer().getCode());
    res.setName(e.getName());
    res.setEmail(e.getEmail());
    res.setPhone(e.getPhone());
    res.setNote(e.getNote());
    res.setIsPrimary(e.getIsPrimary());
    return res;
  }

  // ---- 조회/삭제 ----
  @Transactional(readOnly = true)
  public List<ContactDtos.ContactRes> listByCustomer(String customerCode, String q) {
    var list = contacts.findByCustomer_CodeOrderByIdDesc(customerCode);
    return list.stream().filter(e ->
        q == null || q.isBlank() ||
        (e.getName()!=null && e.getName().toLowerCase().contains(q.toLowerCase())) ||
        (e.getEmail()!=null && e.getEmail().toLowerCase().contains(q.toLowerCase())) ||
        (e.getPhone()!=null && e.getPhone().toLowerCase().contains(q.toLowerCase())) ||
        (e.getNote()!=null && e.getNote().toLowerCase().contains(q.toLowerCase()))
    ).map(e -> {
      var r = new ContactDtos.ContactRes();
      r.id = e.getId();
      r.customerCode = e.getCustomer().getCode();
      r.name = e.getName();
      r.email = e.getEmail();
      r.phone = e.getPhone();
      r.note = e.getNote();
      r.isPrimary = e.getIsPrimary();
      return r;
    }).toList();
  }

  @Transactional(readOnly = true)
  public ContactDtos.ContactRes get(Long id) {
    var e = contacts.findById(id).orElseThrow();
    var r = new ContactDtos.ContactRes();
    r.id = e.getId();
    r.customerCode = e.getCustomer().getCode();
    r.name = e.getName();
    r.email = e.getEmail();
    r.phone = e.getPhone();
    r.note = e.getNote();
    r.isPrimary = e.getIsPrimary();
    return r;
  }

  @Transactional
  public void delete(Long id) { contacts.deleteById(id); }

  @Transactional
  public void deleteByCustomer(String customerCode) {
    contacts.findByCustomer_CodeOrderByIdDesc(customerCode)
        .forEach(contacts::delete);
  }

  // ---- 유틸 ----
  private static String nz(String s) { return s == null ? null : s.trim(); }

  private static String genTempPassword(int length) {
    var rnd = new java.security.SecureRandom();
    var bytes = new byte[Math.max(9, length)];
    rnd.nextBytes(bytes);
    return java.util.Base64.getUrlEncoder().withoutPadding()
        .encodeToString(bytes).substring(0, length);
  }
}
