package com.polarisoffice.secuone.dto;

import jakarta.validation.constraints.NotBlank;

public record NewsletterUpdateReq(
        @NotBlank String title,
        @NotBlank String content
) {}