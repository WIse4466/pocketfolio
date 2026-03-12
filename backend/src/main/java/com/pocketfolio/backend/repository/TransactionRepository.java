package com.pocketfolio.backend.repository;

import com.pocketfolio.backend.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByUserId(UUID userId, Pageable pageable);

    Page<Transaction> findByUserIdAndCategoryId(UUID userId, UUID categoryId, Pageable pageable);

    Page<Transaction> findByUserIdAndAccountId(UUID userId, UUID accountId, Pageable pageable);

    Page<Transaction> findByUserIdAndDateBetween(UUID userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<Transaction> findByUserIdAndAccountIdAndDateBetween(UUID userId, UUID accountId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<Transaction> findByUserIdAndCategoryIdAndDateBetween(UUID userId, UUID categoryId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // 統計：某用戶某帳戶的總金額（含收入/支出判斷)
    @Query("SELECT COALESCE(SUM(CASE " +
            "WHEN t.category IS NULL THEN -t.amount " +
            "WHEN t.category.type = 'INCOME' THEN t.amount " +
            "ELSE -t.amount END), 0) " +
            "FROM Transaction t WHERE t.account.id = :accountId AND t.user.id = :userId")
    BigDecimal calculateNetAmountByAccountIdAndUserId(
            @Param("accountId") UUID accountId,
            @Param("userId") UUID userId
    );

    // 查詢某用戶某年某月的所有交易
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
            "AND YEAR(t.date) = :year AND MONTH(t.date) = :month")
    List<Transaction> findByUserIdAndYearAndMonth(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month
    );
}
