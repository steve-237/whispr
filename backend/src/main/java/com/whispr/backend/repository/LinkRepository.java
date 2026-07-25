package com.whispr.backend.repository;

import com.whispr.backend.domain.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkRepository extends JpaRepository<Link, UUID> {
    Optional<Link> findBySlug(String slug);
    Optional<Link> findByUserId(UUID userId);
    boolean existsBySlug(String slug);
}
