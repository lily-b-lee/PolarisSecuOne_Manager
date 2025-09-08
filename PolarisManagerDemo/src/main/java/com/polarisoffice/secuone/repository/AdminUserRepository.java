// com.polarisoffice.secuone.repository.AdminUserRepository
package com.polarisoffice.secuone.repository;

import com.polarisoffice.secuone.domain.AdminUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUserEntity, Long> {

    boolean existsByUsername(String username);

    Optional<AdminUserEntity> findByUsername(String username);

    Optional<AdminUserEntity> findByUsernameAndIsActiveTrue(String username);
    
    // 엔티티 필드명이 isActive가 아니어도 동작하게 JPQL로 명시
    @Query(value = """
	      SELECT *
	      FROM admin_users
	      WHERE username = :username
	        AND is_active = 1
	      LIMIT 1
	      """, nativeQuery = true)
	  Optional<AdminUserEntity> findActiveByUsername(@Param("username") String username);

    
}
