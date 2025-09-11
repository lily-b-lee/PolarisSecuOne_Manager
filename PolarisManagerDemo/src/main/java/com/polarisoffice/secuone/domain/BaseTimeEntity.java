// src/main/java/com/polarisoffice/secuone/domain/BaseTimeEntity.java
package com.polarisoffice.secuone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) createdAt = LocalDateTime.now();
    if (updatedAt == null) updatedAt = createdAt;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public LocalDateTime getCreatedAt() {
	return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
	this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
	return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
	this.updatedAt = updatedAt;
  }
  
  
}
