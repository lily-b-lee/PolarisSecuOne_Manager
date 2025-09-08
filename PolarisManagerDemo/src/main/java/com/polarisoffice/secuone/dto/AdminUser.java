package com.polarisoffice.secuone.dto;

import java.time.Instant;

import com.polarisoffice.secuone.domain.AdminUserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

public record AdminUser(
	    Long id,
	    String username,
	    String role,
	    Instant createdAt
	) {
	    /** 엔티티 → DTO 매퍼 */
	    public static AdminUser from(AdminUserEntity e) {
	        if (e == null) return null;
	        return new AdminUser(
	            e.getId(),
	            e.getUsername(),
	            e.getRole(),
	            e.getCreatedAt()
	        );
	    }
	}
