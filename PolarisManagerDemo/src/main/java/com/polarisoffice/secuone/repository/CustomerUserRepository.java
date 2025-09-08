package com.polarisoffice.secuone.repository;

import com.polarisoffice.secuone.domain.CustomerUserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerUserRepository extends JpaRepository<CustomerUserEntity, Long> {

  // ───────────────────── 기본/공통 ─────────────────────

  /** me 조회 시 customer까지 필요하므로 즉시 로딩 */
  @Override
  @EntityGraph(attributePaths = "customer")
  Optional<CustomerUserEntity> findById(Long id);

  /** 중복/단건 조회 (customer 즉시 로딩) */
  @EntityGraph(attributePaths = "customer")
  Optional<CustomerUserEntity> findByCustomer_CodeAndUsername(String code, String username);

  Optional<CustomerUserEntity> findByUsername(String username);

  boolean existsByCustomer_CodeAndUsername(String customerCode, String username);

  // username 대소문자 무시 조회(리스트)
  @Query("select u from CustomerUserEntity u where lower(u.username) = lower(:username)")
  List<CustomerUserEntity> findAllByUsernameIgnoreCase(@Param("username") String username);

  // ───────────────────── 로그인 관련(활성) ─────────────────────

  /** 파생 메서드 (camelCase) */
  @EntityGraph(attributePaths = "customer")
  Optional<CustomerUserEntity> findByCustomerCodeAndUsernameAndIsActiveTrue(String customerCode, String username);

  /** 파생 메서드 (customer_code 명시) */
  @EntityGraph(attributePaths = "customer")
  Optional<CustomerUserEntity> findByCustomer_CodeAndUsernameAndIsActiveTrue(String customerCode, String username);

  /** 네이티브(필요 시 사용) — EntityGraph 적용 불가 */
  @Query(value = """
      SELECT *
      FROM customer_users
      WHERE customer_code = :cc
        AND username = :username
        AND is_active = 1
      LIMIT 1
      """, nativeQuery = true)
  Optional<CustomerUserEntity> findActiveByCustomerCodeAndUsername(
      @Param("cc") String customerCode,
      @Param("username") String username
  );

  /** 대소문자 무시 + 활성 */
  @EntityGraph(attributePaths = "customer")
  Optional<CustomerUserEntity> findByCustomerCodeIgnoreCaseAndUsernameIgnoreCaseAndIsActiveTrue(
      String customerCode, String username
  );
}
