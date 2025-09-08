package com.polarisoffice.secuone.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerListItem {
    private Long id;
    private String code;
    private String name;
    private Double cpiRate;
    private Double rsRate;
    private String note;

    // 목록에 보여줄 요약 정보
    private String primaryContactName;
    private String primaryContactEmail;
    private String primaryContactPhone;
    private int    extraContacts; // 추가 인원 수
}