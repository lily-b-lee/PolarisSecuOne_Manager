package com.polarisoffice.secuone.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum NoticeCategory {
  EVENT, EMERGENCY, SERVICE_GUIDE, UPDATE;

  @JsonCreator
  public static NoticeCategory from(String v) {
    if (v == null) return null;
    return NoticeCategory.valueOf(v.trim().toUpperCase());
  }
}