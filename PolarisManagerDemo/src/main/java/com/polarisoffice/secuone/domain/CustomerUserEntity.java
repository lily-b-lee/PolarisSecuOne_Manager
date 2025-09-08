// src/main/java/com/polarisoffice/secuone/domain/CustomerUserEntity.java
package com.polarisoffice.secuone.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "customer_users",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_cu_customer_username",
        columnNames = {"customer_code","username"}
    )
)
@Access(AccessType.FIELD)
public class CustomerUserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // FK: customer_users.customer_code -> customers.code
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "customer_code",
      referencedColumnName = "code",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_cu_customer_code")
  )
  private CustomerEntity customer;

  @Column(name = "username", nullable = false, length = 255)
  private String username;

  @Column(name = "role", nullable = false, length = 50)
  private String role = "VIEWER";

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  // DB가 NOT NULL이면 엔티티도 nullable=false로 맞춰두는 게 안전
  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @PrePersist
  void prePersist() {
    Instant now = Instant.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
    if (role == null) role = "VIEWER";
    if (isActive == null) isActive = true;
    // passwordHash는 서비스단에서 반드시 세팅(임시비번 발급) 후 저장하게 함
  }

  @PreUpdate
  void preUpdate() { updatedAt = Instant.now(); }

  // --- getters/setters ---
  public Long getId() { return id; }
  public CustomerEntity getCustomer() { return customer; }
  public void setCustomer(CustomerEntity customer) { this.customer = customer; }
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean isActive) { this.isActive = isActive; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdateAt(Instant updateAt) {
      this.updatedAt = updateAt;
  }
  public Instant getLastLoginAt() { return lastLoginAt; }
  public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
