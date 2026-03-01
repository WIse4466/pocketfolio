package com.pocketfolio.backend.repository;

import com.pocketfolio.backend.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<Transaction> findByAccountId(UUID accountId, Pageable pageable);

    Page<Transaction> findByDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<Transaction> findByCategoryIdAndDateBetween(
            UUID categoryId, LocalDate startDate, LocalDate endDate, Pageable pageable
    );

    Page<Transaction> findByAccountIdAndDateBetween(
            UUID accountId, LocalDate startDate, LocalDate endDate, Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.category.id = :categoryId")
    BigDecimal sumAmountByCategoryId(@Param("categoryId") UUID categoryId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = :accountId")
    BigDecimal sumAmountByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.category.id = :categoryId AND t.date BETWEEN :StartDate AND :endDate")
    BigDecimal sumAmountByCategoryIdAndDateBetween(
            @Param("categoryId") UUID categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE YEAR(t.date) = :year AND MONTH(t.date) = :month")
    List<Transaction> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
}
