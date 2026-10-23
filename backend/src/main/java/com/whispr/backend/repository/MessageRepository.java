package com.whispr.backend.repository;

import com.whispr.backend.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByLinkIdOrderByCreatedAtDesc(UUID linkId);
    
    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.auditLog WHERE m.link.id = :linkId ORDER BY m.createdAt DESC")
    List<Message> findByLinkIdWithAuditLogOrderByCreatedAtDesc(@Param("linkId") UUID linkId);

    List<Message> findByLinkIdAndStatusOrderByCreatedAtDesc(UUID linkId, String status);
}
