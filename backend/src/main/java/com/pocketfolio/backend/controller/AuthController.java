package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.AuthResponse;
import com.pocketfolio.backend.dto.LoginRequest;
import com.pocketfolio.backend.dto.RegisterRequest;
import com.pocketfolio.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "1. 認證", description = "用戶註冊與登入(無需 Token)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "註冊新用戶",
            description = "建立新的用戶帳號，成功後會回傳 JWT Token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "註冊成功"),
            @ApiResponse(responseCode = "400", description = "Email 已被註冊或資料格式錯誤")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(
            summary = "用戶登入",
            description = "使用 Email 和密碼登入，成功後回傳 JWT Token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登入成功"),
            @ApiResponse(responseCode = "401", description = "Email 或密碼錯誤")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
