package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.CategoryRequest;
import com.pocketfolio.backend.dto.CategoryResponse;
import com.pocketfolio.backend.entity.Category;
import com.pocketfolio.backend.entity.CategoryType;
import com.pocketfolio.backend.entity.User;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
import com.pocketfolio.backend.repository.CategoryRepository;
import com.pocketfolio.backend.security.SecurityUtil;
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
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        // 檢查當前用戶是否已有同名類別
        if (repository.existsByUserIdAndName(currentUserId, request.getName())) {
            throw new IllegalArgumentException("類別名稱「" + request.getName() + "」已存在");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setType(request.getType());
        category.setDescription(request.getDescription());

        // 設定用戶關聯
        User user = new User();
        user.setId(currentUserId);
        category.setUser(user);

        return toResponse(repository.save(category));
    }

    // Read (單筆)
    public CategoryResponse getCategory(UUID id) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的類別"));

        // 驗證類別屬於當前用戶
        if (!category.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + id + " 的類別");
        }

        return toResponse(category);
    }

    // Read (所有)
    public List<CategoryResponse> getAllCategories() {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        return repository.findByUserId(currentUserId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Read (依類型)
    public List<CategoryResponse> getCategoriesByType(CategoryType type) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        return repository.findByUserIdAndType(currentUserId, type).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Update
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的類別"));

        // 驗證類別屬於當前用戶
        if (!category.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + id + " 的類別");
        }

        // 如果改了名稱，檢查新名稱是否與當前用戶的其他類別重複
        if (!category.getName().equals(request.getName())
                && repository.existsByUserIdAndName(currentUserId, request.getName())) {
            throw new IllegalArgumentException("類別名稱「" + request.getName() + "」已存在");
        }

        category.setName(request.getName());
        category.setType(request.getType());
        category.setDescription(request.getDescription());

        return toResponse(repository.save(category));
    }

    // Delete
    public void deleteCategory(UUID id) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的類別"));

        // 驗證類別屬於當前用戶
        if (!category.getUser().getId().equals(currentUserId)) {
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
