package org.project.ttokttok.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

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

        // 서버 정보 설정
        Server prodServer = new Server()
                .url("https://www.hearmeout.kr")
                .description("Production Server");

        Server localServer = new Server()
                .url("http://www.localhost:8080")
                .description("Local Server");

        return new OpenAPI()
                .info(info)
                .servers(List.of(prodServer, localServer)) // 프로덕션과 로컬 서버 정보 모두 추가
                .components(new Components().addSecuritySchemes("cookieAuth", apiKey))
                .security(Collections.singletonList(securityRequirement));
    }

    private Info createSwaggerInfo() {
        return new Info()
                .title("똑똑 게시판 API")
                .description("똑똑 API 문서입니다.")
                .version("0.0.1");
    }
}