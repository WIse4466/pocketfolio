package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.CategoryRequest;
import com.pocketfolio.backend.dto.CategoryResponse;
import com.pocketfolio.backend.entity.CategoryType;
import com.pocketfolio.backend.service.CategoryService;
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
public class CategoryController {

    private final CategoryService service;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request
            ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createCategory(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getCategory(id));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll(
            @RequestParam(required = false) CategoryType type
            ) {
        if (type != null) {
            return ResponseEntity.ok(service.getCategoriesByType(type));
        }
        return ResponseEntity.ok(service.getAllCategories());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity.ok(service.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
