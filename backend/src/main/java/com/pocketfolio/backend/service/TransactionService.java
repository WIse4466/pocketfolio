package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.TransactionRequest;
import com.pocketfolio.backend.dto.TransactionResponse;
import com.pocketfolio.backend.entity.Account;
import com.pocketfolio.backend.entity.Category;
import com.pocketfolio.backend.entity.Transaction;
import com.pocketfolio.backend.entity.User;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
import com.pocketfolio.backend.repository.AccountRepository;
import com.pocketfolio.backend.repository.CategoryRepository;
import com.pocketfolio.backend.repository.TransactionRepository;
import com.pocketfolio.backend.security.SecurityUtil;
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
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        Transaction tx = new Transaction();
        tx.setAmount(request.getAmount());
        tx.setNote(request.getNote());
        tx.setDate(request.getDate() != null ? request.getDate() : LocalDate.now());
        tx.setUser(new User());
        tx.getUser().setId(currentUserId);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "找不到 ID 為 " + request.getCategoryId() + " 的類別"
                    ));
            // 驗證類別屬於當前用戶
            if (!category.getUser().getId().equals(currentUserId)) {
                throw new IllegalArgumentException("無權使用此類別");
            }

            tx.setCategory(category);
        }

        if (request.getAccountId() != null) {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "找不到 ID 為 " + request.getAccountId() + " 的帳戶"
                    ));
            //驗證帳戶屬於當前用戶
            if (!account.getUser().getId().equals(currentUserId)) {
                throw new IllegalArgumentException("無權使用此帳戶");
            }

            tx.setAccount(account);
        }

        return toResponse(repository.save(tx));
    }

    //Read(單筆)
    public TransactionResponse getTransaction(UUID id) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Transaction tx = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的交易"
                ));

        if (!tx.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + id + " 的交易");
        }

        return toResponse(tx);
    }

    //Read(分頁列表)
    //Pageable 由 Controller 傳入，可帶 page / size / sort 參數
    public Page<TransactionResponse> getAllTransactions(Pageable pageable) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        return repository.findByUserId(currentUserId, pageable).map(this::toResponse);
    }

    //Read (依類別)
    public Page<TransactionResponse> getTransactionsByCategory(UUID categoryId, Pageable pageable) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到類別"));
        if (!category.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到類別");
        }

        return repository.findByUserIdAndCategoryId(currentUserId, categoryId, pageable)
                .map(this::toResponse);
    }

    //Read (依帳戶)
    public Page<TransactionResponse> getTransactionsByAccount(UUID accountId, Pageable pageable) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到帳戶"));
        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到帳戶");
        }

        return repository.findByUserIdAndAccountId(currentUserId, accountId, pageable)
                .map(this::toResponse);
    }

    //Read (依日期範圍)
    public Page<TransactionResponse> getTransactionsByDateRange(
            LocalDate startDate, LocalDate endDate, Pageable pageable
    ) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        return repository.findByUserIdAndDateBetween(currentUserId, startDate, endDate, pageable)
                .map(this::toResponse);
    }

    //Read (依帳戶與日期範圍)
    public Page<TransactionResponse> getTransactionsByAccountAndDateRange(
            UUID accountId, LocalDate startDate, LocalDate endDate, Pageable pageable
    ) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        // 驗證帳戶
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到帳戶"));
        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到帳戶");
        }

        return repository.findByUserIdAndAccountIdAndDateBetween(
                        currentUserId, accountId, startDate, endDate, pageable)
                .map(this::toResponse);
    }

    //Read (依類別與日期範圍)
    public Page<TransactionResponse> getTransactionsByCategoryAndDateRange(
            UUID categoryId, LocalDate startDate, LocalDate endDate, Pageable pageable
    ) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        // 驗證類別
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到類別"));
        if (!category.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到類別");
        }

        return repository.findByUserIdAndCategoryIdAndDateBetween(
                        currentUserId, categoryId, startDate, endDate, pageable)
                .map(this::toResponse);
    }

    //Update
    public TransactionResponse updateTransaction(UUID id, TransactionRequest request) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Transaction tx = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的交易"
                ));
        // 驗證交易屬於當前用戶
        if (!tx.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到交易");
        }

        tx.setAmount(request.getAmount());
        tx.setNote(request.getNote());
        if (request.getDate() != null) {
            tx.setDate(request.getDate());
        }

        // 更新類別關聯（需驗證）
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("找不到類別"));
            if (!category.getUser().getId().equals(currentUserId)) {
                throw new IllegalArgumentException("無權使用此類別");
            }
            tx.setCategory(category);
        } else {
            tx.setCategory(null);
        }

        // 更新帳戶關聯（需驗證）
        if (request.getAccountId() != null) {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("找不到帳戶"));
            if (!account.getUser().getId().equals(currentUserId)) {
                throw new IllegalArgumentException("無權使用此帳戶");
            }
            tx.setAccount(account);
        } else {
            tx.setAccount(null);
        }

        return toResponse(repository.save(tx));
    }

    //Delete
    public void deleteTransaction(UUID id) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Transaction tx = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到交易"));

        // 驗證交易屬於當前用戶
        if (!tx.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到交易");
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
