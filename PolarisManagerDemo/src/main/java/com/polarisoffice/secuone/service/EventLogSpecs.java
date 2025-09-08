package com.polarisoffice.secuone.service;

import com.polarisoffice.secuone.domain.EventLogEntity;
import org.springframework.data.jpa.domain.Specification;
import java.time.Instant;

public class EventLogSpecs {
  public static Specification<EventLogEntity> customerCodeEquals(String code) {
    return (root, q, cb) -> cb.equal(root.get("customer").get("code"), code);
  }
  public static Specification<EventLogEntity> between(Instant from, Instant to) {
    return (root, q, cb) -> cb.between(root.get("createdAt"), from, to);
  }
  public static Specification<EventLogEntity> typeEquals(String objectType) {
    return (root, q, cb) -> cb.equal(root.get("objectType"), objectType);
  }
}