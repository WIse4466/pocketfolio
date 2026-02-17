package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.TransactionRequest;
import com.pocketfolio.backend.dto.TransactionResponse;
import com.pocketfolio.backend.entity.Transaction;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
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

    //Create
    public TransactionResponse createTransaction(TransactionRequest request) {
        Transaction tx = new Transaction();
        tx.setAmount(request.getAmount());
        tx.setNote(request.getNote());
        tx.setDate(request.getDate() != null ? request.getDate() : LocalDate.now());

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

    //Update
    public TransactionResponse updateTransaction(UUID id, TransactionRequest request) {
        Transaction tx = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的交易"
                ));
        tx.setAmount(request.getAmount());
        tx.setNote(request.getNote());
        if (request.getDate() != null) tx.setDate(request.getDate());
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
        return TransactionResponse.builder()
                .id(tx.getId())
                .amount(tx.getAmount())
                .note(tx.getNote())
                .date(tx.getDate())
                .build();
    }
}
