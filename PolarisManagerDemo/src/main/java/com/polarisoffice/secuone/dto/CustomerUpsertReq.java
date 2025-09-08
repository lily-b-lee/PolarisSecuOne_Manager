package com.polarisoffice.secuone.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerUpsertReq {
    private Long id;           // null이면 생성
    private String code;
    private String name;
    private Double cpiRate;
    private Double rsRate;
    private String note;
    private List<CustomerContactDto> contacts; // N명
}