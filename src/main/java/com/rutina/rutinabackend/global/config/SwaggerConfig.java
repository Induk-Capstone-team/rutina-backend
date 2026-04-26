package com.rutina.rutinabackend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * Swagger UI 상단 Authorize 버튼에 AccessToken 입력칸 제공
 * - signup/login/check-email → 인증 불필요 (@SecurityRequirements({}) 적용)
 * - reissue/logout → body의 refreshToken으로 자체 검증
 * */
@Configuration
public class SwaggerConfig {

    public static final String ACCESS_TOKEN = "AccessToken";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rutina API")
                        .description("루티나 백엔드 API 명세서")
                        .version("v1"))
                // 모든 API에 AccessToken 인증 표시
                .addSecurityItem(new SecurityRequirement()
                        .addList(ACCESS_TOKEN))
                .components(new Components()
                        .addSecuritySchemes(ACCESS_TOKEN, new SecurityScheme()
                                .name(ACCESS_TOKEN)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인 후 발급받은 Access Token을 입력하세요.")));
    }
}
