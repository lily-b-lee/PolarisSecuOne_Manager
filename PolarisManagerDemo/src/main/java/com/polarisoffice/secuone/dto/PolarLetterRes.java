package com.polarisoffice.secuone.dto;

public record PolarLetterRes(
	    String id,
	    String author,
	    String title,
	    String content,
	    String url,
	    String thumbnail,
	    String createTime
	) {}
