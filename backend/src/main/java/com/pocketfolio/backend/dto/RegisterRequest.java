package com.pocketfolio.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email 不能為空")
    @Email(message = "Email 格式不正確")
    private String email;

    @NotBlank(message = "使用者名稱不能為空")
    @Size(min = 2, max = 50, message = "使用者名稱長度必須在 2-50 之間")
    private String username;

    @NotBlank(message = "密碼不能為空")
    @Size(min = 5, message = "密碼至少需要 6 個字元")
    private String password;
}
