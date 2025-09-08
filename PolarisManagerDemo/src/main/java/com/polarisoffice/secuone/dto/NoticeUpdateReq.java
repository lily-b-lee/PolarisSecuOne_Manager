package com.polarisoffice.secuone.dto;

import com.polarisoffice.secuone.domain.NoticeCategory;

public record NoticeUpdateReq(
	    String author,
	    NoticeCategory category,
	    String title,
	    String content,
	    String date,
	    String imageURL
	) {}
