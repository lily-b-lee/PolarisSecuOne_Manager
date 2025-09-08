package com.polarisoffice.secuone.dto;

import jakarta.validation.constraints.NotBlank;


public record NewsletterRes(
        String id,
        String category,
        String date,       // "2025-04-28" 형식 (문자열로 정렬 가능)
        String thumbnail,  // 이미지 URL
        String title,
        String url
) {}