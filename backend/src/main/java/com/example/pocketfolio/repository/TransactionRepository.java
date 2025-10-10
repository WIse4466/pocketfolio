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

    long countByAccount_Id(UUID accountId);
    long countBySourceAccount_Id(UUID accountId);
    long countByTargetAccount_Id(UUID accountId);

    long countByStatement_IdAndStatus(UUID statementId, com.example.pocketfolio.entity.TransactionStatus status);
}
