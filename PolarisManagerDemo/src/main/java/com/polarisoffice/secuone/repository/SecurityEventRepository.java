// src/main/java/com/polarisoffice/secuone/repository/SecurityEventRepository.java
package com.polarisoffice.secuone.repository;

import com.polarisoffice.secuone.domain.SecurityEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface SecurityEventRepository
extends JpaRepository<SecurityEventEntity, Long>,
        JpaSpecificationExecutor<SecurityEventEntity> {
    

    /** 일별(eventType별) 카운트 프로젝션 */
    interface DailyCountRow {
        LocalDate getDay();     // alias: day
        String    getEventType(); // alias: eventType
        long      getCnt();     // alias: cnt
    }

    /**
     * 고객사 코드 + 기간 필터로 일자/타입별 집계
     * - CAST(created_at AS DATE) 로 날짜만 추출 (MySQL/Postgres/H2 공통 동작)
     */
    @Query(value = """
        select
          cast(e.created_at as date)        as day,
          e.event_type                      as eventType,
          count(*)                          as cnt
        from security_events e
        where e.customer_code = :code
          and e.created_at between :start and :end
        group by cast(e.created_at as date), e.event_type
        order by day asc
        """, nativeQuery = true)
    List<DailyCountRow> countDailyByType(
            @Param("code") String customerCode,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * 악성앱 Top(패키지/유형) 계산용: 기간 내 MALWARE 이벤트 전체 로딩
     * 컨트롤러에서 payload 파싱하여 Top N 계산.
     */
    List<SecurityEventEntity> findByCustomerCodeAndEventTypeAndCreatedAtBetween(
            String customerCode,
            String eventType,
            Instant start,
            Instant end
    );
}
