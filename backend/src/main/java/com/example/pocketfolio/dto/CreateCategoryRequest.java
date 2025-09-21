package com.example.pocketfolio.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateCategoryRequest {
    private String name;
    private UUID parentId;
    
}
