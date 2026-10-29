package com.whispr.backend.repository;

import com.whispr.backend.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId);
    boolean existsByStripeEventId(String stripeEventId);
    boolean existsByStripeSessionId(String stripeSessionId);
    Optional<Transaction> findByStripeSessionId(String stripeSessionId);
}
