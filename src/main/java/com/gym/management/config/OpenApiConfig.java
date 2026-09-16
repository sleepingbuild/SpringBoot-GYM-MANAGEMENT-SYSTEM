package com.gym.management.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 6 sẽ hoàn thiện file này ở Giai đoạn 4 (gom nhóm tag theo module,
 * mô tả chi tiết hơn). Agent 1 dựng khung cơ bản để Swagger UI chạy được từ đầu.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gmsOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Gym Management System (GMS) API")
                        .description("Backend RESTful API cho hệ thống quản lý phòng gym")
                        .version("v0.1.0-dev"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components().addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
