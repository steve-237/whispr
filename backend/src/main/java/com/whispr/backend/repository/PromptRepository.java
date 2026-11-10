package com.whispr.backend.repository;

import com.whispr.backend.domain.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, UUID> {
    List<Prompt> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Prompt> findByUserIdAndIsActiveTrue(UUID userId);
}
