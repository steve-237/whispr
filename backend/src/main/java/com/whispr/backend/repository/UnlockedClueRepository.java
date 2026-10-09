package com.whispr.backend.repository;

import com.whispr.backend.domain.UnlockedClue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnlockedClueRepository extends JpaRepository<UnlockedClue, UUID> {
    List<UnlockedClue> findByUserIdAndMessageId(UUID userId, UUID messageId);
    Optional<UnlockedClue> findByUserIdAndMessageIdAndClueType(UUID userId, UUID messageId, String clueType);
}
