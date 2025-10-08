package com.example.pocketfolio.repository;

import com.example.pocketfolio.entity.Account;
import com.example.pocketfolio.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByType(AccountType type);
}
