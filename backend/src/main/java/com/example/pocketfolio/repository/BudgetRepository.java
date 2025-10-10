package com.example.pocketfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<com.example.pocketfolio.entity.Transaction, UUID> {

    @Query("select coalesce(sum(t.amount),0) from Transaction t where t.userId = :userId and t.kind = com.example.pocketfolio.entity.TransactionKind.EXPENSE and t.occurredAt >= :start and t.occurredAt < :end")
    BigDecimal totalExpense(UUID userId, Instant start, Instant end);

    @Query("select coalesce(sum(t.amount),0) from Transaction t where t.userId = :userId and t.kind = com.example.pocketfolio.entity.TransactionKind.EXPENSE and t.category.id = :categoryId and t.occurredAt >= :start and t.occurredAt < :end")
    BigDecimal categoryExpense(UUID userId, UUID categoryId, Instant start, Instant end);
}

