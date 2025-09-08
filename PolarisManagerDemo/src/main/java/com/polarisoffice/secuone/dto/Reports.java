package com.polarisoffice.secuone.dto;

import java.math.BigDecimal;
import java.util.List;

public class Reports {
  public record MonthlyRes(
      String month,
      long downloads,
      long deletes,
      BigDecimal totalAmount
  ) {}

  public record StatsRes(
      Long customerId,
      String code,
      String name,
      long totalDownloads,
      long totalDeletes,
      BigDecimal totalAmount,
      List<MonthlyRes> monthly
  ) {}
}