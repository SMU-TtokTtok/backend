package org.project.ttokttok.infrastructure.s3.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.infrastructure.s3.exception.S3FileMaxSizeOverException;
import org.project.ttokttok.infrastructure.s3.exception.UnsupportedFileTypeException;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContentValidatorTest {

    @Mock
    private ZipContentValidator zipContentValidator;

    @InjectMocks
    private ContentValidator contentValidator;

    @Nested
    @DisplayName("파일 존재 여부 검증")
    class ValidateContent {

        @Test
        @DisplayName("파일이 존재하면 예외가 발생하지 않는다.")
        void success() {
            MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "content".getBytes());
            assertThatCode(() -> contentValidator.validateContent(file))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ZIP 파일인 경우 ZipContentValidator를 호출한다.")
        void callZipValidator() {
            MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", "content".getBytes());
            contentValidator.validateContent(file);
            verify(zipContentValidator).validateZip(eq(file), any());
        }

        @Test
        @DisplayName("파일이 null이면 예외가 발생한다.")
        void failWhenNull() {
            assertThatThrownBy(() -> contentValidator.validateContent(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일이 비어있거나 존재하지 않습니다.");
        }

        @Test
        @DisplayName("파일이 비어있으면 예외가 발생한다.")
        void failWhenEmpty() {
            MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[0]);
            assertThatThrownBy(() -> contentValidator.validateContent(file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일이 비어있거나 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("파일 크기 검증")
    class ValidateSize {

        @Test
        @DisplayName("파일 크기가 20MB 이하면 예외가 발생하지 않는다.")
        void success() {
            long size = 20 * 1024 * 1024L; // 20MB
            assertThatCode(() -> contentValidator.validateSize(size))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("파일 크기가 20MB를 초과하면 예외가 발생한다.")
        void failWhenSizeExceeded() {
            long size = 20 * 1024 * 1024L + 1; // 20MB + 1B
            assertThatThrownBy(() -> contentValidator.validateSize(size))
                    .isInstanceOf(S3FileMaxSizeOverException.class);
        }
    }

    @Nested
    @DisplayName("파일 형식(MIME Type) 검증")
    class ValidateType {

        @ParameterizedTest
        @ValueSource(strings = {
                "image/jpeg", "image/png", "image/webp", "image/heic", "image/gif",
                "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/csv", "text/plain"
        })
        @DisplayName("허용된 파일 형식이면 예외가 발생하지 않는다.")
        void success(String type) {
            assertThatCode(() -> contentValidator.validateType(type))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("허용되지 않은 파일 형식이면 예외가 발생한다.")
        void failWhenUnsupportedType() {
            String type = "application/x-msdownload"; // .exe 등
            assertThatThrownBy(() -> contentValidator.validateType(type))
                    .isInstanceOf(UnsupportedFileTypeException.class);
        }
    }

    @Nested
    @DisplayName("파일 이름 검증")
    class ValidateFileName {

        @Test
        @DisplayName("유효한 파일 이름이면 예외가 발생하지 않는다.")
        void success() {
            String fileName = "valid-file-name.png";
            assertThatCode(() -> contentValidator.validateFileName(fileName))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "  "})
        @DisplayName("파일 이름이 비어있으면 예외가 발생한다.")
        void failWhenEmpty(String fileName) {
            assertThatThrownBy(() -> contentValidator.validateFileName(fileName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일 이름이 비어있습니다.");
        }

        @Test
        @DisplayName("파일 이름이 255자를 초과하면 예외가 발생한다.")
        void failWhenTooLong() {
            String longFileName = "a".repeat(256);
            assertThatThrownBy(() -> contentValidator.validateFileName(longFileName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일 이름이 너무 깁니다. (최대 255자)");
        }

        @ParameterizedTest
        @ValueSource(strings = {"test/file.png", "test:file.png", "test*file.png", "test?file.png"})
        @DisplayName("파일 이름에 특수 문자가 포함되어 있으면 예외가 발생한다.")
        void failWhenInvalidChars(String fileName) {
            assertThatThrownBy(() -> contentValidator.validateFileName(fileName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일 이름에 허용되지 않는 특수 문자가 포함되어 있습니다.");
        }
    }
}
