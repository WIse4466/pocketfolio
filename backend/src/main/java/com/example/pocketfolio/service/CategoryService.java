package com.example.pocketfolio.service;

import com.example.pocketfolio.dto.CreateCategoryRequest;
import com.example.pocketfolio.dto.CategoryTreeDto;
import com.example.pocketfolio.entity.Category;
import com.example.pocketfolio.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category createCategory(CreateCategoryRequest request) {
        Category newCategory = new Category();
        newCategory.setName(request.getName());


        if (request.getParentId() != null) {
            Category parentCategory = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found with id: " + request.getParentId()));
            newCategory.setParent(parentCategory);
        }

        return categoryRepository.save(newCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeDto> getCategories() {
        List<Category> allCategories = categoryRepository.findAll();
        Map<UUID, CategoryTreeDto> categoryMap = allCategories.stream()
                .map(category -> CategoryTreeDto.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .children(new ArrayList<>())
                        .build())
                .collect(Collectors.toMap(CategoryTreeDto::getId, c -> c));

        List<CategoryTreeDto> rootCategories = new ArrayList<>();
        for (Category category : allCategories) {
            CategoryTreeDto categoryDto = categoryMap.get(category.getId());
            if (category.getParent() != null) {
                CategoryTreeDto parentDto = categoryMap.get(category.getParent().getId());
                if (parentDto != null) {
                    parentDto.getChildren().add(categoryDto);
                }
            } else {
                rootCategories.add(categoryDto);
            }
        }
        return rootCategories;
    }

    @Transactional
    public void deleteCategory(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Category not found with id: " + id);
        }
        // Note: This will fail if the category has children due to foreign key constraints.
        // A more robust implementation would handle this case.
        categoryRepository.deleteById(id);
    }
}
