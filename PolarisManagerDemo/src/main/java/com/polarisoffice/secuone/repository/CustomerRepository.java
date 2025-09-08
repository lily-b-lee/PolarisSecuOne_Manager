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

    boolean existsByCode(String code);
    Optional<CustomerEntity> findByCode(String code);

    // ✅ 추가: 코드 대소문자 무시
    Optional<CustomerEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    @Query("""
      select c from CustomerEntity c
       where (:q is null
              or lower(c.code) like lower(concat('%', :q, '%'))
              or lower(c.name) like lower(concat('%', :q, '%'))
              or lower(c.integrationType) like lower(concat('%', :q, '%')) )
       order by c.code asc
    """)
    List<CustomerEntity> search(@Param("q") String q);

    @Query("""
      select c from CustomerEntity c
       where (:q is null
              or lower(c.code) like lower(concat('%', :q, '%'))
              or lower(c.name) like lower(concat('%', :q, '%'))
              or lower(c.integrationType) like lower(concat('%', :q, '%')) )
    """)
    Page<CustomerEntity> search(@Param("q") String q, Pageable pageable);

    Optional<CustomerEntity> findByDomainIgnoreCase(String domain);
    boolean existsByDomainIgnoreCase(String domain);
}
