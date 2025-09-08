package com.polarisoffice.secuone.dto;

import com.polarisoffice.secuone.domain.NoticeCategory;

import jakarta.validation.constraints.NotBlank;

public record NoticeCreateReq(
	    String author,
	    NoticeCategory category,    // EVENT | EMERGENCY | SERVICE_GUIDE | UPDATE
	    @NotBlank String title,
	    String content,
	    String date,                // 없으면 오늘 "yyyy.MM.dd"
	    String imageURL
	) {}
