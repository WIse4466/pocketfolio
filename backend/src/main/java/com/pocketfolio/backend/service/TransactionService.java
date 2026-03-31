package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.TransactionRequest;
import com.pocketfolio.backend.dto.TransactionResponse;
import com.pocketfolio.backend.entity.Account;
import com.pocketfolio.backend.entity.Category;
import com.pocketfolio.backend.entity.Transaction;
import com.pocketfolio.backend.entity.TransactionType;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    //Create
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        if (request.getType() == TransactionType.TRANSFER_OUT) {
            return createTransfer(request);
        }

        UUID currentUserId = SecurityUtil.getCurrentUserId();
        Transaction tx = buildBase(request, currentUserId);
        tx.setType(request.getType());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "找不到 ID 為 " + request.getCategoryId() + " 的類別"
                    ));
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
            if (!account.getUser().getId().equals(currentUserId)) {
                throw new IllegalArgumentException("無權使用此帳戶");
            }
            tx.setAccount(account);
        }

        return toResponse(repository.save(tx));
    }

    // 建立一組轉帳（TRANSFER_OUT + TRANSFER_IN），共用同一個 transferGroupId
    private TransactionResponse createTransfer(TransactionRequest request) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        if (request.getAccountId() == null || request.getToAccountId() == null) {
            throw new IllegalArgumentException("轉帳需要來源帳戶與目標帳戶");
        }
        if (request.getAccountId().equals(request.getToAccountId())) {
            throw new IllegalArgumentException("來源帳戶與目標帳戶不能相同");
        }

        Account fromAccount = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到來源帳戶"));
        if (!fromAccount.getUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("無權使用此帳戶");
        }

        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到目標帳戶"));
        if (!toAccount.getUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("無權使用此帳戶");
        }

        UUID groupId = UUID.randomUUID();

        Transaction out = buildBase(request, currentUserId);
        out.setType(TransactionType.TRANSFER_OUT);
        out.setAccount(fromAccount);
        out.setTransferGroupId(groupId);

        Transaction in = buildBase(request, currentUserId);
        in.setType(TransactionType.TRANSFER_IN);
        in.setAccount(toAccount);
        in.setTransferGroupId(groupId);

        repository.save(in);
        Transaction savedOut = repository.save(out);

        // Response 以 TRANSFER_OUT 為主，附帶目標帳戶資訊
        TransactionResponse response = toResponse(savedOut);
        response.setToAccountId(toAccount.getId());
        response.setToAccountName(toAccount.getName());
        return response;
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

        return toResponseWithTransferInfo(tx);
    }

    //Read(分頁列表)
    public Page<TransactionResponse> getAllTransactions(Pageable pageable) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        return repository.findByUserId(currentUserId, pageable).map(this::toResponseWithTransferInfo);
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
                .map(this::toResponseWithTransferInfo);
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
                .map(this::toResponseWithTransferInfo);
    }

    //Read (依日期範圍)
    public Page<TransactionResponse> getTransactionsByDateRange(
            LocalDate startDate, LocalDate endDate, Pageable pageable
    ) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        return repository.findByUserIdAndDateBetween(currentUserId, startDate, endDate, pageable)
                .map(this::toResponseWithTransferInfo);
    }

    //Read (依帳戶與日期範圍)
    public Page<TransactionResponse> getTransactionsByAccountAndDateRange(
            UUID accountId, LocalDate startDate, LocalDate endDate, Pageable pageable
    ) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到帳戶"));
        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到帳戶");
        }

        return repository.findByUserIdAndAccountIdAndDateBetween(
                        currentUserId, accountId, startDate, endDate, pageable)
                .map(this::toResponseWithTransferInfo);
    }

    //Read (依類別與日期範圍)
    public Page<TransactionResponse> getTransactionsByCategoryAndDateRange(
            UUID categoryId, LocalDate startDate, LocalDate endDate, Pageable pageable
    ) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到類別"));
        if (!category.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到類別");
        }

        return repository.findByUserIdAndCategoryIdAndDateBetween(
                        currentUserId, categoryId, startDate, endDate, pageable)
                .map(this::toResponseWithTransferInfo);
    }

    //Update（轉帳不支援編輯，需刪除重建）
    @Transactional
    public TransactionResponse updateTransaction(UUID id, TransactionRequest request) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Transaction tx = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的交易"
                ));
        if (!tx.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到交易");
        }
        if (tx.getType() == TransactionType.TRANSFER_OUT || tx.getType() == TransactionType.TRANSFER_IN) {
            throw new IllegalArgumentException("轉帳記錄不支援編輯，請刪除後重新建立");
        }

        tx.setType(request.getType());
        tx.setAmount(request.getAmount());
        tx.setNote(request.getNote());
        if (request.getDate() != null) {
            tx.setDate(request.getDate());
        }

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

    //Delete（轉帳連帶刪除配對記錄）
    @Transactional
    public void deleteTransaction(UUID id) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Transaction tx = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到交易"));

        if (!tx.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到交易");
        }

        if (tx.getTransferGroupId() != null) {
            repository.findByTransferGroupIdAndIdNot(tx.getTransferGroupId(), id)
                    .ifPresent(repository::delete);
        }

        repository.deleteById(id);
    }

    // Helper: 建立基礎 Transaction（不含 type / account / category）
    private Transaction buildBase(TransactionRequest request, UUID userId) {
        Transaction tx = new Transaction();
        tx.setAmount(request.getAmount());
        tx.setNote(request.getNote());
        tx.setDate(request.getDate() != null ? request.getDate() : LocalDate.now());
        User user = new User();
        user.setId(userId);
        tx.setUser(user);
        return tx;
    }

    // Helper: Entity → DTO（一般交易）
    private TransactionResponse toResponse(Transaction tx) {
        TransactionResponse.TransactionResponseBuilder builder = TransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType())
                .amount(tx.getAmount())
                .note(tx.getNote())
                .date(tx.getDate())
                .transferGroupId(tx.getTransferGroupId());

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

    // Helper: Entity → DTO（含轉帳目標帳戶查詢）
    private TransactionResponse toResponseWithTransferInfo(Transaction tx) {
        TransactionResponse response = toResponse(tx);

        if (tx.getType() == TransactionType.TRANSFER_OUT && tx.getTransferGroupId() != null) {
            repository.findByTransferGroupIdAndIdNot(tx.getTransferGroupId(), tx.getId())
                    .ifPresent(paired -> {
                        if (paired.getAccount() != null) {
                            response.setToAccountId(paired.getAccount().getId());
                            response.setToAccountName(paired.getAccount().getName());
                        }
                    });
        }

        return response;
    }
}
