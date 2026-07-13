package org.project.ttokttok.global.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * 파일 업로드 크기 한계를 코드로 관리하는 설정.
 *
 * <p>업로드 사이즈 한계는 보안·정책과 직결되므로, 환경별로 관리되는(그리고 git에서
 * 추적하지 않는) {@code application*.yml} 대신 버전 관리되는 이 클래스에서 단일 소스로
 * 정의한다. 여기서 정의한 {@link MultipartConfigElement} 빈은 Spring Boot의 기본
 * multipart 자동 설정을 대체하므로, yml의 {@code spring.servlet.multipart.*} 사이즈
 * 값보다 이 설정이 우선한다.
 *
 * <p>{@code ContentValidator}의 애플리케이션 레벨 사이즈 검증도 {@link #MAX_FILE_SIZE}를
 * 참조하여, 서블릿 컨테이너 한계와 애플리케이션 검증 한계가 절대 어긋나지 않도록 한다.
 */
@Configuration
public class MultipartConfig {

    /** 파일 1개의 최대 크기. 프론트엔드 업로드 제한(20MB)과 일치시킨다. */
    public static final DataSize MAX_FILE_SIZE = DataSize.ofMegabytes(20);

    /** 하나의 요청 전체(다중 첨부 합산)의 최대 크기. */
    public static final DataSize MAX_REQUEST_SIZE = DataSize.ofMegabytes(100);

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(MAX_FILE_SIZE);
        factory.setMaxRequestSize(MAX_REQUEST_SIZE);
        return factory.createMultipartConfig();
    }
}
