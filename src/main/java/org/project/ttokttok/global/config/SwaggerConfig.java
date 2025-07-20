package org.project.ttokttok.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.servers.Server;

import java.util.Collections;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.url}")
    private String prodUrl;

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

        Server prodServer = new Server()
                .url("https://hearmeout.kr")
                .description("Production Server");

        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local Server");

        return new OpenAPI()
                .info(info)
                .servers(List.of(prodServer, localServer))
                .components(new Components().addSecuritySchemes("cookieAuth", apiKey))
                .security(Collections.singletonList(securityRequirement));
    }

    // todo: 추후 수정
    private Info createSwaggerInfo() {
        return new Info()
                .title("똑똑 게시판 API")
                .description("똑똑 API 문서입니다.")
                .version("0.0.1");
    }
}
