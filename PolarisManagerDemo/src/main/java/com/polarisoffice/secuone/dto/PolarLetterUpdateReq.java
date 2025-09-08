package com.polarisoffice.secuone.dto;

public record PolarLetterUpdateReq(
	    String author,
	    String title,
	    String content,
	    String url,
	    String thumbnail,
	    String createTime
	) {}
