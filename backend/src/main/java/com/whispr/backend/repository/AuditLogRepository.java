package com.whispr.backend.repository;

import com.whispr.backend.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Optional<AuditLog> findByMessageId(UUID messageId);
    int countByRawIpAndCreatedAtAfter(String rawIp, LocalDateTime date);
}
