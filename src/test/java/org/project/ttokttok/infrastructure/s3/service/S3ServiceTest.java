package org.project.ttokttok.infrastructure.s3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        s3Service = new S3Service(s3Client, null, null); // S3Presigner는 null로 설정
    }

    @Test
    @DisplayName("S3 클라이언트 연결 테스트")
    void testS3Connection() {
        // Mock S3Client의 동작 정의
        when(s3Client.listBuckets(ListBucketsRequest.builder().build()))
                .thenReturn(ListBucketsResponse.builder().build());

        // S3Client의 listBuckets 호출
        ListBucketsResponse response = s3Client.listBuckets(ListBucketsRequest.builder().build());

        // 응답이 null이 아닌지 확인
        assertNotNull(response);

        // listBuckets 메서드가 호출되었는지 검증
        verify(s3Client, times(1)).listBuckets(any(ListBucketsRequest.class));
    }
}