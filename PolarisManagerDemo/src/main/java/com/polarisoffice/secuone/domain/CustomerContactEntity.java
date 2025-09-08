package com.polarisoffice.secuone.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
  name = "customer_contacts",
  indexes = @Index(name = "ix_cc_customer_code", columnList = "customer_code")
)
@Access(AccessType.FIELD)
public class CustomerContactEntity {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // FK: customer_contacts.customer_code -> customers.code
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "customer_code",
      referencedColumnName = "code",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_cc_customer_code")
  )
  private CustomerEntity customer;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "email", length = 255)
  private String email;

  @Column(name = "phone", length = 50)
  private String phone;

  // DB 컬럼은 memo 이지만 코드에선 note 로 사용
  @Lob
  @Column(name = "memo", columnDefinition = "text")
  private String note;

  @Column(name = "role", length = 50)
  private String role;

  @Column(name = "is_primary", nullable = false)
  private Boolean isPrimary = false;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    Instant now = Instant.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
    if (isPrimary == null) isPrimary = false;
  }
  @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

  // --- getters/setters ---
  public Long getId() { return id; }
  public CustomerEntity getCustomer() { return customer; }
  public void setCustomer(CustomerEntity customer) { this.customer = customer; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public Boolean getIsPrimary() { return isPrimary; }
  public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
