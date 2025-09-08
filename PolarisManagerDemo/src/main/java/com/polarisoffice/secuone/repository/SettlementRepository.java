package com.polarisoffice.secuone.repository;

import com.polarisoffice.secuone.domain.SettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SettlementRepository extends JpaRepository<SettlementEntity, Long> {

  /**
   * 고객사 코드와 월 범위로 정산 행 조회
   * settleMonth가 "YYYY-MM" 문자열이라고 가정 (문자열 비교로 범위 OK)
   */
  @Query("""
     select s from SettlementEntity s
      where (:customerCode is null or s.customer.code = :customerCode)
        and (:fromMonth   is null or s.settleMonth >= :fromMonth)
        and (:toMonth     is null or s.settleMonth   <= :toMonth)
      order by s.customer.code asc, s.settleMonth desc
  """)
  List<SettlementEntity> findByFiltersCode(
      @Param("customerCode") String customerCode,
      @Param("fromMonth") String fromMonth,
      @Param("toMonth") String toMonth
  );
}
