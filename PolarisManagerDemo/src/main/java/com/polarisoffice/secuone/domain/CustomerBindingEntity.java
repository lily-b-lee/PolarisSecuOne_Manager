// src/main/java/com/polarisoffice/secuone/domain/CustomerBindingEntity.java
package com.polarisoffice.secuone.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customer_bindings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"type","key"}))
public class CustomerBindingEntity {
  public enum BindingType { APP, WEB }

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 고객사 코드에 FK (CustomerEntity.code를 PK/UK로 쓰고 있다고 가정)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_code", referencedColumnName = "code")
  private CustomerEntity customer;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private BindingType type;       // APP | WEB

  @Column(name = "`key`", nullable = false, length = 255)
  private String key;             // APP: 패키지명 (e.g. com.foo.bar)
                                  // WEB: 호스트명 or 패턴 (e.g. portal.example.com, %.example.com)

  @Column(nullable = false)
  private Boolean isActive = true;

  @Column(nullable = false)
  private Integer priority = 0;   // 중복 매칭 시 우선순위 (높을수록 우선)

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate void onUpdate(){ this.updatedAt = Instant.now(); }

  // getters/setters ...
  public Long getId() { return id; }
  public CustomerEntity getCustomer() { return customer; }
  public void setCustomer(CustomerEntity c) { this.customer = c; }
  public BindingType getType() { return type; }
  public void setType(BindingType type) { this.type = type; }
  public String getKey() { return key; }
  public void setKey(String key) { this.key = key == null ? null : key.toLowerCase(); }
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean isActive) { this.isActive = isActive; }
  public Integer getPriority() { return priority; }
  public void setPriority(Integer priority) { this.priority = priority == null ? 0 : priority; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
