package com.example.pocketfolio.repository;

import com.example.pocketfolio.entity.Statement;
import com.example.pocketfolio.entity.StatementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

@Repository
public interface StatementRepository extends JpaRepository<Statement, UUID> {
    List<Statement> findByAccountIdAndClosingDate(UUID accountId, LocalDate closingDate);
    List<Statement> findByDueDateAndStatus(LocalDate dueDate, StatementStatus status);
    List<Statement> findByDueDateBetween(LocalDate from, LocalDate to);
    List<Statement> findByDueDateBetweenAndAccountId(LocalDate from, LocalDate to, UUID accountId);
}
