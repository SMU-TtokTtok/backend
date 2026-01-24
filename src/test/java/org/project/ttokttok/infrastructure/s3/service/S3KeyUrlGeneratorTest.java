package org.project.ttokttok.infrastructure.s3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.project.ttokttok.infrastructure.s3.enums.S3FileDirectory;
import org.springframework.test.util.ReflectionTestUtils;

import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class S3KeyUrlGeneratorTest {

    private S3KeyUrlGenerator generator;

    // 공통 테스트 상수
    private static final String TEST_BASE_URL = "https://test-cdn.example.com";
    private static final String DEFAULT_FILE_NAME = "test.jpg";
    private static final String UUID_PATTERN = "[a-f0-9-]{36}";

    @BeforeEach
    void setUp() {
        generator = new S3KeyUrlGenerator();
        ReflectionTestUtils.setField(generator, "fileCloudUrl", TEST_BASE_URL);
    }

    @Test
    @DisplayName("키 생성 - 기본 형식 검증")
    void generateKey_ShouldCreateValidFormat() {
        // Given
        String dirName = S3FileDirectory.PROFILE_IMAGE.getDirectoryName();

        // When
        String result = generator.generateKey(dirName, DEFAULT_FILE_NAME);

        // Then
        assertValidKeyFormat(result, dirName, DEFAULT_FILE_NAME);
    }

    @Test
    @DisplayName("URL 생성 - 키로부터 올바른 URL 조합")
    void createUrl_ShouldCombineBaseUrlWithKey() {
        // Given
        String dirName = S3FileDirectory.PROFILE_IMAGE.getDirectoryName();
        String key = dirName + "12345678-1234-1234-1234-123456789abc_" + DEFAULT_FILE_NAME;

        // When
        String result = generator.createUrl(key);

        // Then
        assertThat(result)
                .as("생성된 URL")
                .isEqualTo(TEST_BASE_URL + "/" + key)
                .startsWith(TEST_BASE_URL)
                .contains(key);
    }

    @Test
    @DisplayName("키 추출 - 유효한 URL에서 키 분리")
    void extractKeyFromUrl_ShouldExtractKeyFromValidUrl() {
        // Given
        String dirName = S3FileDirectory.BOARD_IMAGE.getDirectoryName();
        String key = dirName + "uuid_" + DEFAULT_FILE_NAME;
        String url = TEST_BASE_URL + "/" + key;

        // When
        String result = generator.extractKeyFromUrl(url);

        // Then
        assertThat(result)
                .as("추출된 키")
                .isEqualTo(key)
                .startsWith(dirName)
                .endsWith(DEFAULT_FILE_NAME);
    }

    @Test
    @DisplayName("키 추출 실패 - 잘못된 도메인 URL")
    void extractKeyFromUrl_ShouldThrowExceptionForInvalidUrl() {
        // Given
        String invalidUrl = "https://wrong-domain.com/some-file.jpg";

        // When & Then
        assertThatThrownBy(() -> generator.extractKeyFromUrl(invalidUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid CloudFront URL format");
    }

    @Test
    @DisplayName("기본 URL 반환 - 설정된 Base URL 확인")
    void getBaseUrl_ShouldReturnConfiguredBaseUrl() {
        // When
        String result = generator.getBaseUrl();

        // Then
        assertThat(result)
                .as("설정된 기본 URL")
                .isEqualTo(TEST_BASE_URL)
                .startsWith("https://")
                .contains("test-cdn");
    }

    @Test
    @DisplayName("키 생성 고유성 - 동일 입력에 대해 다른 키 생성")
    void generateKey_ShouldGenerateUniqueKeysForSameInput() {
        // Given
        String dirName = S3FileDirectory.INTRODUCTION_IMAGE.getDirectoryName();

        // When
        String key1 = generator.generateKey(dirName, DEFAULT_FILE_NAME);
        String key2 = generator.generateKey(dirName, DEFAULT_FILE_NAME);
        String key3 = generator.generateKey(dirName, DEFAULT_FILE_NAME);

        // Then
        assertThat(List.of(key1, key2, key3))
                .as("생성된 3개의 키들")
                .hasSize(3)
                .doesNotHaveDuplicates()
                .allSatisfy(key -> assertValidKeyFormat(key, dirName, DEFAULT_FILE_NAME));
    }

    @ParameterizedTest
    @EnumSource(S3FileDirectory.class)
    @DisplayName("모든 S3FileDirectory enum 값과의 호환성 검증")
    void generateKey_ShouldWorkWithAllDirectoryTypes(S3FileDirectory directory) {
        // Given
        String dirName = directory.getDirectoryName();

        // When
        String generatedKey = generator.generateKey(dirName, DEFAULT_FILE_NAME);

        // Then
        assertValidKeyFormat(generatedKey, dirName, DEFAULT_FILE_NAME);
        assertUrlKeyConsistency(generatedKey);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "normal-file.jpg",
        "파일 이름 with spaces & symbols.jpg",
        "file_with_underscores.png",
        "file.with.dots.pdf",
        "한글파일명.docx"
    })
    @DisplayName("다양한 파일명 형식 처리 검증")
    void generateKey_ShouldHandleVariousFileNames(String fileName) {
        // Given
        String dirName = S3FileDirectory.BOARD_FILE.getDirectoryName();

        // When
        String result = generator.generateKey(dirName, fileName);

        // Then
        assertValidKeyFormat(result, dirName, fileName);
        assertUrlKeyConsistency(result);
    }

    // Helper Methods
    private void assertValidKeyFormat(String key, String dirName, String fileName) {
        assertThat(key)
                .as("생성된 S3 키: %s", key)
                .startsWith(dirName)
                .endsWith("_" + fileName)
                .satisfies(k -> {
                    String uuidPart = k.substring(dirName.length(), k.length() - fileName.length() - 1);
                    assertThat(uuidPart)
                            .as("UUID 부분: %s", uuidPart)
                            .matches(UUID_PATTERN);
                });
    }

    private void assertUrlKeyConsistency(String key) {
        String url = generator.createUrl(key);
        String extractedKey = generator.extractKeyFromUrl(url);

        assertThat(extractedKey)
                .as("URL 생성/키 추출 일관성 검증 - 원본 키: %s, 추출된 키: %s", key, extractedKey)
                .isEqualTo(key);
    }
}
