package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.KnownAssetResponse;
import com.pocketfolio.backend.repository.KnownAssetRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/known-assets")
@RequiredArgsConstructor
@Tag(name = "7. 資產搜尋", description = "搜尋已知股票與加密貨幣清單（Autocomplete 用）")
@SecurityRequirement(name = "bearerAuth")
public class KnownAssetController {

    private final KnownAssetRepository knownAssetRepository;

    @GetMapping("/search")
    @Operation(
            summary = "搜尋資產",
            description = "依關鍵字搜尋資產代碼或名稱，回傳最多 20 筆。assetType: STOCK_TW / STOCK_TWO / CRYPTO"
    )
    public List<KnownAssetResponse> search(
            @RequestParam String assetType,
            @RequestParam String keyword) {

        return knownAssetRepository.searchByKeyword(assetType, keyword, PageRequest.of(0, 20))
                .stream()
                .map(KnownAssetResponse::from)
                .toList();
    }
}
