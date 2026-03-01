package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.TransactionRequest;
import com.pocketfolio.backend.dto.TransactionResponse;
import com.pocketfolio.backend.entity.Account;
import com.pocketfolio.backend.entity.Category;
import com.pocketfolio.backend.entity.Transaction;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
import com.pocketfolio.backend.repository.AccountRepository;
import com.pocketfolio.backend.repository.CategoryRepository;
import com.pocketfolio.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    //Create
    public TransactionResponse createTransaction(TransactionRequest request) {
        Transaction tx = new Transaction();
        tx.setAmount(request.getAmount());
        tx.setNote(request.getNote());
        tx.setDate(request.getDate() != null ? request.getDate() : LocalDate.now());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "找不到 ID 為 " + request.getCategoryId() + " 的類別"
                    ));
            tx.setCategory(category);
        }

        if (request.getAccountId() != null) {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "找不到 ID 為 " + request.getAccountId() + " 的帳戶"
                    ));
            tx.setAccount(account);
        }

        return toResponse(repository.save(tx));
    }

    //Read(單筆)
    public TransactionResponse getTransaction(UUID id) {
        Transaction tx = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的交易"
                ));
        return toResponse(tx);
    }

    //Read(分頁列表)
    //Pageable 由 Controller 傳入，可帶 page / size / sort 參數
    public Page<TransactionResponse> getAllTransactions(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    //Read (依類別)
    public Page<TransactionResponse> getTransactionsByCategory(UUID categroyId, Pageable pageable) {
        if (!categoryRepository.existsById(categroyId)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + categroyId + " 的類別");
        }
        return repository.findByCategoryId(categroyId, pageable).map(this::toResponse);
    }

    //Read (依帳戶)
    public Page<TransactionResponse> getTransactionsByAccount(UUID accountId, Pageable pageable) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + accountId + " 的帳戶");
        }
        return repository.findByAccountId(accountId, pageable).map(this::toResponse);
    }

    //Read (依日期範圍)
    public Page<TransactionResponse> getTransactionsByDateRange(
            LocalDate startDate, LocalDate endDate, Pageable pageable
    ) {
        return repository.findByDateBetween(startDate, endDate, pageable).map(this::toResponse);
    }

    //Read (依帳戶與日期範圍)
    public Page<TransactionResponse> getTransactionsByAccountAndDateRange(
            UUID accountId, LocalDate startDate, LocalDate endDate, Pageable pageable
    ) {
        if(!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + accountId + " 的帳戶");
        }
        return repository.findByAccountIdAndDateBetween(accountId, startDate, endDate, pageable).map(this::toResponse);
    }

    //Read (依類別與日期範圍)
    public Page<TransactionResponse> getTransactionsByCategoryAndDateRange(
            UUID categoryId, LocalDate startDate, LocalDate endDate, Pageable pageable
    ) {
        if(!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + categoryId + " 的類別");
        }
        return repository.findByCategoryIdAndDateBetween(categoryId, startDate, endDate, pageable).map(this::toResponse);
    }

    //Update
    public TransactionResponse updateTransaction(UUID id, TransactionRequest request) {
        Transaction tx = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的交易"
                ));
        tx.setAmount(request.getAmount());
        tx.setNote(request.getNote());
        if (request.getDate() != null) tx.setDate(request.getDate());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "找不到 ID 為 " + request.getCategoryId() + " 的類別"
                    ));
            tx.setCategory(category);
        } else {
            tx.setCategory(null);
        }

        if (request.getAccountId() != null) {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "找不到 ID 為 " + request.getAccountId() + " 的類別"
                    ));
            tx.setAccount(account);
        } else {
            tx.setAccount(null);
        }

        return toResponse(repository.save(tx));
    }

    //Delete
    public void deleteTransaction(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + id + " 的交易");
        }
        repository.deleteById(id);
    }

    //Helper: Entity → DTO
    private TransactionResponse toResponse(Transaction tx) {
        TransactionResponse.TransactionResponseBuilder builder =  TransactionResponse.builder()
                .id(tx.getId())
                .amount(tx.getAmount())
                .note(tx.getNote())
                .date(tx.getDate());

        if (tx.getCategory() != null) {
            builder.categoryId(tx.getCategory().getId())
                    .categoryName(tx.getCategory().getName())
                    .categoryType(tx.getCategory().getType());
        }

        if (tx.getAccount() != null) {
            builder.accountId(tx.getAccount().getId())
                    .accountName(tx.getAccount().getName())
                    .accountType(tx.getAccount().getType());
        }

        return builder.build();
    }
}
