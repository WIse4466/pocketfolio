package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.CreateTransactionRequest;
import com.pocketfolio.backend.entity.Transaction;
import com.pocketfolio.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository repository;

    public Transaction createTransaction(CreateTransactionRequest request) {
        Transaction tx = new Transaction();
        tx.setAmount(request.getAmount());
        tx.setNote(request.getNote());
        tx.setDate(request.getDate() != null ? request.getDate() : LocalDate.now());

        return repository.save(tx);
    }
}
