// src/main/java/com/polarisoffice/secuone/repository/CustomerRepository.java
package com.polarisoffice.secuone.repository;

import com.polarisoffice.secuone.domain.CustomerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // ✅ 추가

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

public interface CustomerRepository extends JpaRepository<CustomerEntity, String> {


	  // ===== 존재/조회 (code) =====
	  boolean existsByCode(String code);
	  boolean existsByCodeIgnoreCase(String code);

	  @Nullable
	  CustomerEntity findByCode(String code);

	  Optional<CustomerEntity> findByCodeIgnoreCase(String code);

	  // ====== 도메인 관련 ======
	  // 1) 도메인으로 엔티티 조회 (선호)
	  Optional<CustomerEntity> findByDomainIgnoreCase(String domain);

	  // 2) 도메인으로 코드만 조회 (원-라이너 필요 시)
	  @Query("select c.code from CustomerEntity c where lower(c.domain) = lower(:domain)")
	  Optional<String> findCodeByDomainIgnoreCase(@Param("domain") String domain);

	  // (옵션) 존재 여부
	  boolean existsByDomainIgnoreCase(String domain);

	  // ====== 검색 ======
	  @Query("""
	      SELECT c
	      FROM CustomerEntity c
	      WHERE (:q IS NULL OR :q = '' OR
	             LOWER(c.code) LIKE LOWER(CONCAT('%', :q, '%')) OR
	             LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
	      ORDER BY c.code ASC
	      """)
	  List<CustomerEntity> search(@Param("q") String q);

	  @Query("""
	      SELECT c
	      FROM CustomerEntity c
	      WHERE (:q IS NULL OR :q = '' OR
	             LOWER(c.code) LIKE LOWER(CONCAT('%', :q, '%')) OR
	             LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
	      """)
	  Page<CustomerEntity> search(@Param("q") String q, Pageable pageable);
	}
