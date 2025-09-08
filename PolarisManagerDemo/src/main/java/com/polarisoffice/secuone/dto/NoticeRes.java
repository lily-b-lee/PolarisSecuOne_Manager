package com.polarisoffice.secuone.dto;

import com.polarisoffice.secuone.domain.NoticeCategory;

public record NoticeRes(
	    String id,
	    String author,
	    NoticeCategory category,
	    String title,
	    String content,
	    String date,      // "yyyy.MM.dd"
	    String imageURL
	) {}
