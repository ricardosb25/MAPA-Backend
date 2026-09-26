package com.mapa.repository;

import com.mapa.domain.UserAuditLog;
import com.mapa.domain.enums.UserAuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {

    Page<UserAuditLog> findAllBy(Pageable pageable);

    Page<UserAuditLog> findByAction(UserAuditAction action, Pageable pageable);

    Page<UserAuditLog> findByTargetUserId(Long targetUserId, Pageable pageable);

    Page<UserAuditLog> findByActionAndTargetUserId(UserAuditAction action, Long targetUserId, Pageable pageable);
}
