package com.whispr.backend.repository;

import com.whispr.backend.domain.BlockedSender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlockedSenderRepository extends JpaRepository<BlockedSender, UUID> {
    boolean existsByUserIdAndBlockedHashedIp(UUID userId, String blockedHashedIp);
    List<BlockedSender> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<BlockedSender> findByIdAndUserId(UUID id, UUID userId);
    void deleteByIdAndUserId(UUID id, UUID userId);
}
