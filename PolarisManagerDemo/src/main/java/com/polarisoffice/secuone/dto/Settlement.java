package com.polarisoffice.secuone.dto;


import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class Settlement {

	 public record CreateReq(Long customerId, String settleMonth,
             Long downloads, Long deletes,
             BigDecimal cpiRate, BigDecimal rsRate,
             String currency, String memo) {}
	public record UpdateReq(Long downloads, Long deletes,
	             BigDecimal cpiRate, BigDecimal rsRate,
	             String currency, String memo) {}
	public record Res(Long id, Long customerId, String customerCode, String customerName,
	       String settleMonth, Long downloads, Long deletes,
	       BigDecimal cpiRate, BigDecimal rsRate,
	       BigDecimal cpiAmount, BigDecimal rsAmount,
	       BigDecimal totalAmount, String currency, String memo) {}
	}

