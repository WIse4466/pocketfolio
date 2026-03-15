// src/main/java/com/pocketfolio/backend/config/OpenApiConfig.java
package com.pocketfolio.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("PocketFolio API")
                        .version("1.0.0")
                        .description("""
                                個人財務管理系統 API 文檔
                                
                                功能包含：
                                - 用戶認證（JWT）
                                - 交易記錄管理
                                - 帳戶管理
                                - 資產追蹤
                                - 即時價格更新
                                - 統計分析
                                """)
                        .contact(new Contact()
                                .name("PocketFolio Team")
                                .email("support@pocketfolio.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("請在此輸入 JWT Token（登入後取得）")));
    }
}