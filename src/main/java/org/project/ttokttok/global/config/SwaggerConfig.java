package org.project.ttokttok.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server; // Server 클래스 임포트
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List; // List 임포트

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI boardAPI() {
        Info info = createSwaggerInfo();

        // API Key 기반의 쿠키 인증 설정
        SecurityScheme apiKey = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("ttac"); // Access Token을 담는 쿠키 이름

        // 보안 요구사항 설정
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("cookieAuth");

        // --- 이 부분 추가/수정 ---
        // 서버 URL을 명시적으로 설정 (하드코딩)
        // application.yml 설정이 적용되지 않는 문제를 우회하기 위함
        Server server = new Server().url("https://www.hearmeout.kr").description("Production Server");
        // --- 여기까지 ---

        return new OpenAPI()
                .info(info)
                .components(new Components().addSecuritySchemes("cookieAuth", apiKey))
                .security(Collections.singletonList(securityRequirement))
                .servers(List.of(server)); // 서버 설정 추가
    }

    // todo: 추후 수정
    private Info createSwaggerInfo() {
        return new Info()
                .title("똑똑 게시판 API")
                .description("똑똑 API 문서입니다.")
                .version("0.0.1");
    }
}
