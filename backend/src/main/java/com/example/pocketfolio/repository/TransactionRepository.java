package com.example.pocketfolio.repository;

import com.example.pocketfolio.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByUserIdAndOccurredAtBetweenOrderByOccurredAtAsc(UUID userId, Instant from, Instant to, Pageable pageable);

    // Traverse nested property: account.id
    List<Transaction> findByAccount_IdAndOccurredAtBetween(UUID accountId, Instant from, Instant to);
}
