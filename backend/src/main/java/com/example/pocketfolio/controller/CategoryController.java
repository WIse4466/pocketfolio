package com.example.pocketfolio.controller;

import com.example.pocketfolio.dto.CreateCategoryRequest;
import com.example.pocketfolio.dto.CategoryTreeDto;
import com.example.pocketfolio.entity.Category;
import com.example.pocketfolio.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryTreeDto> createCategory(@RequestBody CreateCategoryRequest request) {
        Category createdCategory = categoryService.createCategory(request);
        CategoryTreeDto responseDto = CategoryTreeDto.builder()
                .id(createdCategory.getId())
                .name(createdCategory.getName())
                .children(new ArrayList<>()) // A new category has no children yet
                .build();
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CategoryTreeDto>> getCategories() {
        List<CategoryTreeDto> categories = categoryService.getCategories();
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
