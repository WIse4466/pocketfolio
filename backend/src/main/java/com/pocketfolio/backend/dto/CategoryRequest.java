package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "類別名稱不能為空")
    private String name;

    @NotNull(message = "類別類型不能為空")
    private CategoryType type;

    private String description;
}
