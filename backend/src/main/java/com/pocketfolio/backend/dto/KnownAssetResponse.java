package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.KnownAsset;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnownAssetResponse {

    private String symbol;
    private String name;
    private String displayCode;
    private String assetType;
    private Integer marketCapRank;

    public static KnownAssetResponse from(KnownAsset ka) {
        return KnownAssetResponse.builder()
                .symbol(ka.getSymbol())
                .name(ka.getName())
                .displayCode(ka.getDisplayCode())
                .assetType(ka.getAssetType())
                .marketCapRank(ka.getMarketCapRank())
                .build();
    }
}
