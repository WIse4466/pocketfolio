package com.pocketfolio.backend.repository;

import com.pocketfolio.backend.entity.Category;
import com.pocketfolio.backend.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // 依類型查詢（查詢所有收入類別 或 所有支出類別）
    List<Category> findByType(CategoryType type);

    // 依名稱查詢（檢查重複）
    boolean existsByName(String name);
}
