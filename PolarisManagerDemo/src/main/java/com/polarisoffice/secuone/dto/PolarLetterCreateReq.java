package com.polarisoffice.secuone.dto;

import jakarta.validation.constraints.NotBlank;

public record PolarLetterCreateReq(
	    String author,
	    @NotBlank String title,
	    String content,
	    String url,
	    String thumbnail,
	    String createTime  // 없으면 서버가 오늘 날짜 "yyyy.MM.dd" 로 채움
	) {}
