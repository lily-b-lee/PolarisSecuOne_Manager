package com.polarisoffice.secuone.dto;

import java.time.LocalDateTime;

public record EventLogView (
	    Long id,
	    LocalDateTime createdAt,
	    String type,
	    String deviceId,
	    String sourcePackage,
	    String payload,
	    String evidenceCode	
	) {

    public Long id() {
        return id;
    }

        public LocalDateTime createdAt() {
        return createdAt;
    }

        public String type() {
        return type;
    }

        public String deviceId() {
        return deviceId;
    }

        public String sourcePackage() {
        return sourcePackage;
    }

        public String payload() {
        return payload;
    }

        public String evidenceCode() {
        return evidenceCode;
    }
    
    
}