package com.mapa.repository;

import com.mapa.domain.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAuditLogRepository
        extends JpaRepository<UserAuditLog, Long>, JpaSpecificationExecutor<UserAuditLog> {
}