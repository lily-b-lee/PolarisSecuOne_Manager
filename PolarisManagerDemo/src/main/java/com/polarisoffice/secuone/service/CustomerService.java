package com.polarisoffice.secuone.service;

import com.polarisoffice.secuone.dto.CustomerDtos.*;
import com.polarisoffice.secuone.domain.CustomerEntity;
import com.polarisoffice.secuone.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

  private final CustomerRepository customerRepository = null;

  @Transactional
  public String create(CustomerReq req) {
    if (req.code == null || req.code.isBlank()) {
      throw new IllegalArgumentException("code는 필수입니다.");
    }
    if (customerRepository.existsByCode(req.code)) {
      throw new IllegalArgumentException("이미 존재하는 code입니다: " + req.code);
    }
    CustomerEntity e = toEntity(new CustomerEntity(), req);
    e.setCode(req.code);
    return customerRepository.save(e).getCode();
  }

  @Transactional
  public String update(String code, CustomerReq req) {
    CustomerEntity e = customerRepository.findById(code)
        .orElseThrow(() -> new IllegalArgumentException("고객사가 없습니다: " + code));
    toEntity(e, req);
    return customerRepository.save(e).getCode();
  }

  @Transactional(readOnly = true)
  public CustomerRes get(String code) {
    CustomerEntity e = customerRepository.findById(code)
        .orElseThrow(() -> new IllegalArgumentException("고객사가 없습니다: " + code));
    return toRes(e);
  }

  @Transactional(readOnly = true)
  public List<CustomerListItem> search(String q) {
    return customerRepository.search(q).stream().map(this::toListItem).toList();
  }

  @Transactional(readOnly = true)
  public Page<CustomerListItem> search(String q, Pageable pageable) {
    return customerRepository.search(q, pageable).map(this::toListItem);
  }

  @Transactional
  public void delete(String code) {
    if (!customerRepository.existsByCode(code)) return;
    customerRepository.deleteById(code);
  }

  /* --------- mapper --------- */
  private CustomerEntity toEntity(CustomerEntity e, CustomerReq req) {
    e.setName(req.name);
    e.setIntegrationType(req.integrationType);
    e.setRsPercent(req.rsPercent == null ? java.math.BigDecimal.ZERO : req.rsPercent);
    e.setCpiValue(req.cpiValue == null ? java.math.BigDecimal.ZERO : req.cpiValue);
    e.setNote(req.note);
    return e;
  }

  private CustomerRes toRes(CustomerEntity e) {
    CustomerRes r = new CustomerRes();
    r.code = e.getCode();
    r.name = e.getName();
    r.integrationType = e.getIntegrationType();
    r.rsPercent = e.getRsPercent();
    r.cpiValue = e.getCpiValue();
    r.note = e.getNote();
    r.createdAt = e.getCreatedAt();
    r.updatedAt = e.getUpdatedAt();
    return r;
  }

  private CustomerListItem toListItem(CustomerEntity e) {
    CustomerListItem r = new CustomerListItem();
    r.code = e.getCode();
    r.name = e.getName();
    r.integrationType = e.getIntegrationType();
    r.rsPercent = e.getRsPercent();
    r.cpiValue = e.getCpiValue();
    return r;
  }
}