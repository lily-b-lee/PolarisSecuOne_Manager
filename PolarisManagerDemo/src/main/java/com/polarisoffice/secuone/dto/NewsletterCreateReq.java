package com.polarisoffice.secuone.dto;

import jakarta.validation.constraints.NotBlank;

public record NewsletterCreateReq(
		String category,
		String date,
		@NotBlank String title,
		String url,
		String thumbnail
) {}
