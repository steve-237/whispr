package com.whispr.backend.repository;

import com.whispr.backend.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.time.ZonedDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Optional<AuditLog> findByMessageId(UUID messageId);
    int countByRawIpAndCreatedAtAfter(String rawIp, ZonedDateTime date);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM AuditLog a WHERE a.message.link.user.id = :userId ORDER BY a.createdAt DESC")
    java.util.List<AuditLog> findByUserId(@org.springframework.data.repository.query.Param("userId") UUID userId);
}

