package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.CategoryRequest;
import com.pocketfolio.backend.dto.CategoryResponse;
import com.pocketfolio.backend.entity.CategoryType;
import com.pocketfolio.backend.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "3. 類別管理", description = "管理收入與支出類別")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService service;

    @PostMapping
    @Operation(summary = "建立類別", description = "新增一個收入或支出類別")
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request
            ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createCategory(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查詢單個類別", description = "根據 ID 查詢類別詳情")
    public ResponseEntity<CategoryResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getCategory(id));
    }

    @GetMapping
    @Operation(
            summary = "查詢類別列表",
            description = "查詢所有類別，可依類型（收入/支出）篩選"
    )
    public ResponseEntity<List<CategoryResponse>> getAll(
            @RequestParam(required = false) CategoryType type
            ) {
        if (type != null) {
            return ResponseEntity.ok(service.getCategoriesByType(type));
        }
        return ResponseEntity.ok(service.getAllCategories());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新類別", description = "修改指定類別的內容")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity.ok(service.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除類別", description = "刪除指定的類別")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
