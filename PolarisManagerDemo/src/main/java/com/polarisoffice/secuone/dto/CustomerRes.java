package com.polarisoffice.secuone.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerRes {
    private Long id;
    private String code;
    private String name;
    private Double cpiRate;
    private Double rsRate;
    private String note;
    private List<CustomerContactDto> contacts; // 전체 리스트
}