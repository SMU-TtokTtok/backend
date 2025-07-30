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

import java.util.Collections;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Bean
    public OpenAPI boardAPI() {
        Info info = createSwaggerInfo();

        // 쿠키 기반 인증 설정
        SecurityScheme cookieAuth = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("ttac") // Access Token 쿠키
                .description("로그인 후 자동으로 설정되는 액세스 토큰 쿠키");

        // 리프레시 토큰 쿠키 설정
        SecurityScheme refreshCookieAuth = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("ttref") // Refresh Token 쿠키
                .description("로그인 후 자동으로 설정되는 리프레시 토큰 쿠키");

        // 보안 요구사항 설정
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("cookieAuth")
                .addList("refreshCookieAuth");

        // 환경별 서버 설정
        List<Server> servers = createServersByEnvironment();

        return new OpenAPI()
                .info(info)
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes("cookieAuth", cookieAuth)
                        .addSecuritySchemes("refreshCookieAuth", refreshCookieAuth))
                .security(Collections.singletonList(securityRequirement));
    }

    private List<Server> createServersByEnvironment() {
        if ("prod".equals(activeProfile)) {
            // 프로덕션 환경: 프로덕션 서버를 첫 번째로 설정
            return List.of(
                new Server()
                    .url("https://www.hearmeout.kr")
                    .description("Production Server (기본값)"),
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server")
            );
        } else {
            // 개발 환경: 로컬 서버를 첫 번째로 설정
            return List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server (기본값)"),
                new Server()
                    .url("https://www.hearmeout.kr")
                    .description("Production Server")
            );
        }
    }

    private Info createSwaggerInfo() {
        return new Info()
                .title("똑똑 게시판 API")
                .description("""
                        똑똑 API 문서입니다.
                        
                        ## 현재 환경: %s
                        
                        ## 인증 방법
                        1. 로그인 API를 호출합니다
                        2. 응답으로 받은 토큰을 브라우저 콘솔에서 쿠키로 설정합니다:
                        
                        ```javascript
                        // 로그인 응답에서 토큰 복사 후 실행
                        document.cookie = "ttac=액세스토큰; path=/; max-age=900; SameSite=Lax";
                        document.cookie = "ttref=리프레시토큰; path=/; max-age=604800; SameSite=Lax";
                        ```
                        
                        3. 이후 API 호출 시 자동으로 쿠키가 전송됩니다
                        
                        ## 서버 전환
                        - 개발 환경: Local Development Server 사용 권장
                        - 프로덕션 환경: Production Server 사용 권장
                        """.formatted(activeProfile))
                .version("0.0.1");
    }
}