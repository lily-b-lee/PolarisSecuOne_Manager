package com.polarisoffice.secuone.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "settlements",
    uniqueConstraints = @UniqueConstraint(
        name="uk_settle_customer_month",
        columnNames = {"customer_code","settle_month"}
    )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SettlementEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(
      name = "customer_code",
      referencedColumnName = "code",
      nullable = false,
      foreignKey = @ForeignKey(name="fk_settle_customer_code")
  )
  private CustomerEntity customer;

  @Column(name = "settle_month", length = 7, nullable = false)
  private String settleMonth;

  @Column(name = "downloads")
  private Long downloads;

  @Column(name = "deletes")
  private Long deletes;

  // --- 요율/금액 ---
  @Column(name = "cpi_rate", precision = 6, scale = 2)
  private BigDecimal cpiRate;

  @Column(name = "rs_rate", precision = 6, scale = 2)
  private BigDecimal rsRate;

  @Column(name = "cpi_amount", precision = 18, scale = 2)
  private BigDecimal cpiAmount;

  @Column(name = "rs_amount", precision = 18, scale = 2)
  private BigDecimal rsAmount;

  @Column(name = "total_amount", precision = 18, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "currency", length = 3)
  private String currency;

  @Column(name = "memo", columnDefinition = "TEXT")
  private String memo;

  // ⬇⬇ 여기만 핵심 수정: 빌더 기본값 보존
  @Builder.Default
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = Instant.now();
  }

}
