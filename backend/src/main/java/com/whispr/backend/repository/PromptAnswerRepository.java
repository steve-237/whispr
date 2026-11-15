package com.whispr.backend.repository;

import com.whispr.backend.domain.PromptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PromptAnswerRepository extends JpaRepository<PromptAnswer, UUID> {
    boolean existsByPromptIdAndHashedIp(UUID promptId, String hashedIp);
}
