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

public interface CustomerRepository extends JpaRepository<CustomerEntity, String> {

	  // 대소문자 무시 존재 체크
	  boolean existsByCodeIgnoreCase(String code);

	  // 검색 (code/name 부분 일치)
	  @Query("""
	      select c from CustomerEntity c
	      where lower(c.code) like lower(concat('%', :q, '%'))
	         or lower(c.name) like lower(concat('%', :q, '%'))
	      order by c.code asc
	      """)
	  List<CustomerEntity> search(@Param("q") String q);
	}
