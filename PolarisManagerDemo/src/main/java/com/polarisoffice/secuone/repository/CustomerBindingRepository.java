// src/main/java/com/polarisoffice/secuone/repository/CustomerBindingRepository.java
package com.polarisoffice.secuone.repository;

import com.polarisoffice.secuone.domain.CustomerBindingEntity;
import com.polarisoffice.secuone.domain.CustomerBindingEntity.BindingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerBindingRepository extends JpaRepository<CustomerBindingEntity, Long> {

  Optional<CustomerBindingEntity> findFirstByTypeAndKeyIgnoreCaseAndIsActiveTrueOrderByPriorityDesc(
      BindingType type, String key);

  // WEB: 패턴 매칭 (예: host = "a.b.example.com"이면 key가 "%.example.com" 인 row 매칭)
  @Query("""
      select b
      from CustomerBindingEntity b
      where b.isActive = true
        and b.type = :type
        and ( lower(b.key) = lower(:host) or lower(:host) like lower(b.key) )
      order by b.priority desc
      """)
  List<CustomerBindingEntity> findWebMatches(BindingType type, String host);
}
