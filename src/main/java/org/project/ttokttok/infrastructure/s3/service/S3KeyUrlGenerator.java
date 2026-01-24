package org.project.ttokttok.infrastructure.s3.service;

import static org.project.ttokttok.global.exception.ErrorMessage.S3_DELETE_NOT_FOUND;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class S3KeyUrlGenerator {

    @Value("${file-cloud.url}")
    private String fileCloudUrl;

    private static final String UNDERSCORE = "_";
    private static final String SLASH = "/";

    /**
     * S3 키를 생성합니다.
     * 형식: {dirName}{uuid}_{fileName}
     */
    public String generateKey(String dirName, String fileName) {
        return dirName + UUID.randomUUID() + UNDERSCORE + fileName;
    }

    /**
     * S3 키로부터 CloudFront URL을 생성합니다.
     */
    public String createUrl(String key) {
        return fileCloudUrl + SLASH + key;
    }

    /**
     * CloudFront URL에서 S3 키를 추출합니다.
     */
    public String extractKeyFromUrl(String url) {
        if (url.startsWith(fileCloudUrl + SLASH)) {
            return url.substring(fileCloudUrl.length() + 1);
        }
        throw new IllegalArgumentException(S3_DELETE_NOT_FOUND.getMessage());
    }

    /**
     * 기본 CloudFront URL을 반환합니다.
     */
    public String getBaseUrl() {
        return fileCloudUrl;
    }
}
