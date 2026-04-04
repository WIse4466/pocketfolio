package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AssetRequest {

    @NotNull(message = "帳戶 ID 不能為空")
    private UUID accountId;

    @NotNull(message = "資產類型不能為空")
    private AssetType type;

    @NotBlank(message = "代號不能為空")
    private String symbol;  // 例如：2330.TW、BTC

    @NotBlank(message = "名稱不能為空")
    private String name;  // 例如：台積電、比特幣

    @NotNull(message = "數量不能為空")
    @Positive(message = "數量必須大於 0")
    private BigDecimal quantity;

    @NotNull(message = "成本價不能為空")
    @Positive(message = "成本價必須大於 0")
    private BigDecimal costPrice;

    private String note;

    // 選填：購買時從哪個帳戶扣款（若填寫，自動建立轉帳記錄）
    private UUID fromAccountId;
}