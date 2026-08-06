package org.project.ttokttok.infrastructure.s3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    /**
     * S3 호환 스토리지(MinIO 등)의 엔드포인트.
     *
     * <p>비어 있으면 실제 AWS S3 에 붙는다 — 기존 운영/로컬/테스트 동작을 그대로
     * 유지하기 위한 기본값이다. 값이 주어지면 해당 엔드포인트로 붙으면서 path-style
     * 접근으로 전환한다. MinIO 는 버킷을 호스트명(virtual-hosted style)이 아니라 경로에
     * 두기 때문에, 엔드포인트만 바꾸고 path-style 을 켜지 않으면 버킷명이 DNS 로
     * 해석되지 않아 요청이 실패한다.
     */
    @Value("${cloud.aws.s3.endpoint:}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());

        if (hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(pathStyleConfiguration());
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());

        if (hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(pathStyleConfiguration());
        }

        return builder.build();
    }

    private boolean hasCustomEndpoint() {
        return StringUtils.hasText(endpoint);
    }

    private AwsCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    private S3Configuration pathStyleConfiguration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
    }
}
