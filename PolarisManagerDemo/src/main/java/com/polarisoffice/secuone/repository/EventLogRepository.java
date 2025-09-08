package com.polarisoffice.secuone.repository;

import com.polarisoffice.secuone.domain.EventLogEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

public interface EventLogRepository
    extends JpaRepository<EventLogEntity, Long>, JpaSpecificationExecutor<EventLogEntity> {

  // 기존 의미를 현재 스키마로 치환
  boolean existsByActionAndActorAndObjectId(String action, String actor, String objectId);

  Optional<EventLogEntity> findTopByActionAndActorAndObjectIdOrderByCreatedAtAsc(
      String action, String actor, String objectId);

  @Query("""
	    select e from EventLogEntity e
	     where upper(e.customer.code) = upper(:code)
	       and e.createdAt between :from and :to
	     order by e.createdAt desc
	  """)
	  List<EventLogEntity> findAllByCustomerAndRange(
	      @Param("code") String code,
	      @Param("from") Instant from,
	      @Param("to")   Instant to
	  );

	  @Query("""
	    select e from EventLogEntity e
	     where upper(e.customer.code) = upper(:code)
	       and e.createdAt between :from and :to
	       and e.objectType = :type
	     order by e.createdAt desc
	  """)
	  List<EventLogEntity> findByType(
	      @Param("code") String code,
	      @Param("from") Instant from,
	      @Param("to")   Instant to,
	      @Param("type") String type
	  );

	  @Query("""
	    select e.objectType, count(e)
	      from EventLogEntity e
	     where upper(e.customer.code) = upper(:code)
	       and e.createdAt between :from and :to
	     group by e.objectType
	  """)
	  List<Object[]> countByType(
	      @Param("code") String code,
	      @Param("from") Instant from,
	      @Param("to")   Instant to
	  );
}
