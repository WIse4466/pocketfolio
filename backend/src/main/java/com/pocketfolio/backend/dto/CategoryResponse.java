package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.CategoryType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CategoryResponse {
    private UUID id;
    private String name;
    private CategoryType type;
    private String description;
}
