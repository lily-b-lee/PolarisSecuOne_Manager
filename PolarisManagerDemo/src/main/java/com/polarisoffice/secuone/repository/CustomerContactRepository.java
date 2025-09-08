package com.polarisoffice.secuone.repository;

import com.polarisoffice.secuone.domain.CustomerContactEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerContactRepository extends JpaRepository<CustomerContactEntity, Long> {

    @Query("""
	    select c from CustomerContactEntity c
	     where (:code is null or c.customer.code = :code)
	       and (:q is null or (
	            lower(c.name) like lower(concat('%', :q, '%'))
	         or lower(c.email) like lower(concat('%', :q, '%'))
	         or lower(c.phone) like lower(concat('%', :q, '%'))
	       ))
	     order by c.id desc
	  """)
  List<CustomerContactEntity> search(@Param("code") String customerCode,
                                     @Param("q") String keyword);

  void deleteByCustomer_Code(String customerCode);
  List<CustomerContactEntity> findByCustomer_Code(String customerCode);
  List<CustomerContactEntity> findByCustomer_CodeOrderByIdDesc(String customerCode);
  // 고객사+이메일 일치 (username이 이메일일 때 매칭 용도)
  Optional<CustomerContactEntity> findFirstByCustomer_CodeAndEmailIgnoreCase(String customerCode, String email);

  // 고객사 대표(주담당자)
  Optional<CustomerContactEntity> findFirstByCustomer_CodeAndIsPrimaryTrueOrderByIdAsc(String customerCode);

  Optional<CustomerContactEntity> findTopByCustomer_CodeOrderByIdAsc(String code);
}
