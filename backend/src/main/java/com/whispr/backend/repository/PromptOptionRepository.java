package com.whispr.backend.repository;

import com.whispr.backend.domain.PromptOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PromptOptionRepository extends JpaRepository<PromptOption, UUID> {
    List<PromptOption> findByPromptIdOrderByDisplayOrderAsc(UUID promptId);
}
