package org.project.ttokttok.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI boardAPI() {
        Info info = createSwaggerInfo();

        return new OpenAPI()
                .info(info)
                .components(new Components());
    }

    // todo: 추후 수정
    private Info createSwaggerInfo() {
        return new Info()
                .title("똑똑 게시판 API")
                .description("똑똑 API 문서입니다.")
                .version("0.0.1");
    }
}