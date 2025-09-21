package com.example.pocketfolio.service;

import com.example.pocketfolio.dto.CreateCategoryRequest;
import com.example.pocketfolio.entity.Category;
import com.example.pocketfolio.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
