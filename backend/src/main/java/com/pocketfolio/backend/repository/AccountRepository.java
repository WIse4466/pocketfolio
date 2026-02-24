package com.pocketfolio.backend.repository;

import com.pocketfolio.backend.entity.Account;
import com.pocketfolio.backend.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    // 依類型查詢
    List<Account> findByType(AccountType type);

    // 依名稱查詢（檢查重複）
    boolean existsByName(String name);

    // 依名稱模糊查詢（例如搜尋「台新」會找到「台新銀行」）
    List<Account> findByNameContainingIgnoreCase(String keyword);
}