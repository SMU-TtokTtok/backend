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

        // Bearer Token 인증 설정 (관리자 및 사용자 공통)
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        로그인 후 받은 AccessToken을 입력하세요. (Bearer 접두사는 자동 추가됩니다)
                        <br/><br/>
                        <b>📋 사용 방법:</b><br/>
                        <b>1️⃣</b> 관리자 테스트: /api/admin/auth/login 호출 → accessToken 복사 → 여기에 입력<br/>
                        <b>2️⃣</b> 사용자 테스트: /api/auth/login 호출 → accessToken 복사 → 여기에 입력<br/>
                        <b>3️⃣</b> 역할 전환: 다른 계정으로 로그인 → 새 토큰으로 교체<br/>
                        <br/>
                        <b>⚠️ 주의:</b> 토큰에 따라 접근 가능한 API가 다릅니다.<br/>
                        • 관리자 토큰: /api/admin/** 접근 가능<br/>
                        • 사용자 토큰: /api/admin/** 접근 불가 (403 Forbidden)
                        """);

        // 보안 요구사항 설정
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        // 환경별 서버 설정
        List<Server> servers = createServersByEnvironment();

        return new OpenAPI()
                .info(info)
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerAuth))
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
                        <br/><br/>
                        <h2>현재 환경: %s</h2>
                        
                        <h2>인증 방법</h2>
                        <ol>
                        <li>로그인 API를 호출합니다 (관리자: <code>/api/admin/auth/login</code>, 사용자: <code>/api/user/auth/login</code>)</li>
                        <li>응답 JSON에서 <code>accessToken</code>과 <code>refreshToken</code>을 받습니다</li>
                        <li>Swagger UI 우상단의 🔒(Authorize) 버튼을 클릭합니다</li>
                        <li>"bearerAuth" 섹션에 <code>accessToken</code> 값을 입력합니다 (Bearer 접두사 제외)</li>
                        <li>이후 모든 API 호출에 자동으로 Authorization 헤더가 추가됩니다</li>
                        </ol>
                        
                        <b>참고</b>: Authorization 헤더 형태: <code>Authorization: Bearer {accessToken}</code>
                        
                        <h2>서버 전환</h2>
                        <ul>
                        <li>개발 환경: Local Development Server 사용 권장</li>
                        <li>프로덕션 환경: Production Server 사용 권장</li>
                        </ul>
                        """.formatted(activeProfile))
                .version("0.0.1");
    }
}