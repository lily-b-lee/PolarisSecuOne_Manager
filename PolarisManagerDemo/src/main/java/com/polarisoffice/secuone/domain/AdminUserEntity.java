package com.polarisoffice.secuone.domain;

import jakarta.persistence.*;
import java.time.Instant;


@Entity
@Table(name = "admin_users",
       indexes = { @Index(name="ix_admin_users_username", columnList="username", unique = true) })
public class AdminUserEntity {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false, length=60, unique = true)
  private String username;

  @Column(nullable=false, length=255)
  private String passwordHash;

  @Column(nullable=false, length=40)
  private String role = "ADMIN";

  @Column(nullable=false)
  private Boolean isActive = true;

  @Column(nullable=false)
  private Instant createdAt = Instant.now();

  private Instant lastLoginAt;

  // ===== getters/setters =====
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }

  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean isActive) { this.isActive = isActive; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getLastLoginAt() { return lastLoginAt; }
  public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}