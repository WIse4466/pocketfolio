package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.CategoryRequest;
import com.pocketfolio.backend.dto.CategoryResponse;
import com.pocketfolio.backend.entity.Category;
import com.pocketfolio.backend.entity.CategoryType;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
import com.pocketfolio.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;

    // Create
    public CategoryResponse createCategory(CategoryRequest request) {
        if (repository.existsByName(request.getName())) {
            throw new IllegalArgumentException("類別名稱「" + request.getName() + "」已存在");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setType(request.getType());
        category.setDescription(request.getDescription());

        return toResponse(repository.save(category));
    }

    // Read (單筆)
    public CategoryResponse getCategory(UUID id) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的類別"
                ));
        return toResponse(category);
    }

    // Read (所有)
    public List<CategoryResponse> getAllCategories() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Read (依類型)
    public List<CategoryResponse> getCategoriesByType(CategoryType type) {
        return repository.findByType(type).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Update
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的類別"
                ));

        if (!category.getName().equals(request.getName())
                && repository.existsByName(request.getName())) {
            throw new IllegalArgumentException("類別名稱「" + request.getName() + "」已存在");
        }

        category.setName(request.getName());
        category.setType(request.getType());
        category.setDescription(request.getDescription());

        return toResponse(repository.save(category));
    }

    // Delete
    public void deleteCategory(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + id + " 的類別");
        }
        repository.deleteById(id);
    }

    // Helper
    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .description(category.getDescription())
                .build();
    }
}
