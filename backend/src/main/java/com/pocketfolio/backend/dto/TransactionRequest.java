package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.AssetType;
import com.pocketfolio.backend.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TransactionRequest {
    @NotNull(message = "金額不能為空")
    private BigDecimal amount;

    private String note;

    private LocalDate date;

    @NotNull(message = "交易類型不能為空")
    private TransactionType type;

    // INCOME / EXPENSE 使用；TRANSFER 不需要
    private UUID categoryId;

    // 來源帳戶（所有類型）
    private UUID accountId;

    // 目標帳戶（TRANSFER_OUT 類型使用）
    private UUID toAccountId;

    // 資產連結（TRANSFER_OUT 轉入 INVESTMENT 帳戶時選填）
    private UUID assetId;            // 現有資產 ID → 加倉
    private AssetType assetType;     // 新資產類型（建立新資產時使用）
    private String assetSymbol;      // 新資產代號
    private String assetName;        // 新資產名稱
    private BigDecimal assetQuantity;    // 數量（加倉或新增皆用）
    private BigDecimal assetCostPrice;   // 單價（加倉或新增皆用；amount 由前端以 quantity × unitPrice 帶入）
    private String assetNote;
}
