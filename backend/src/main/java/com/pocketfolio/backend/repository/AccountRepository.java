package com.pocketfolio.backend.repository;

import com.pocketfolio.backend.entity.Account;
import com.pocketfolio.backend.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    // 依用戶查詢
    List<Account> findByUserId(UUID userId);

    // 依類型查詢
    List<Account> findByUserIdAndType(UUID userId, AccountType type);

    // 依名稱查詢（檢查重複）
    boolean existsByUserIdAndName(UUID userId, String name);

    // 依名稱模糊查詢（例如搜尋「台新」會找到「台新銀行」）
    List<Account> findByUserIdAndNameContainingIgnoreCase(UUID userId, String keyword);
}