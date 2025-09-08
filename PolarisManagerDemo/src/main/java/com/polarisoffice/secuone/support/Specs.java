// src/main/java/com/polarisoffice/secuone/support/Specs.java
package com.polarisoffice.secuone.support;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import java.time.Instant;

public final class Specs {
    private Specs() {}

    public static <E> Specification<E> eq(String field, Object value) {
        return (root, q, cb) -> (value == null) ? null : cb.equal(root.get(field), value);
    }

    public static <E, Y extends Comparable<? super Y>>
    Specification<E> between(String field, Y from, Y to, Class<Y> type) {
        return (root, q, cb) -> {
            if (from == null && to == null) return null;
            Expression<Y> expr = root.get(field).as(type);
            if (from != null && to != null) return cb.between(expr, from, to);
            if (from != null)               return cb.greaterThanOrEqualTo(expr, from);
            return cb.lessThanOrEqualTo(expr, to);
        };
    }

    // ✅ Instant 전용 헬퍼
    public static <E> Specification<E> betweenInstant(String field, Instant from, Instant to) {
        return between(field, from, to, Instant.class);
    }
}
