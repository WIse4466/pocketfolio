package com.pocketfolio.backend.repository;

import com.pocketfolio.backend.entity.Category;
import com.pocketfolio.backend.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByUserId(UUID userId);

    List<Category> findByUserIdAndType(UUID userId, CategoryType type);

    // 檢查用戶是否有同名類別
    boolean existsByUserIdAndName(UUID userId, String name);
}
