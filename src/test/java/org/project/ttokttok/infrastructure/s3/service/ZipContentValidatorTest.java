package org.project.ttokttok.infrastructure.s3.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZipContentValidatorTest {

    private final ZipContentValidator zipContentValidator = new ZipContentValidator();
    private final Set<String> allowedExtensions = Set.of("image/png", "application/pdf");

    @Test
    @DisplayName("정상적인 ZIP 파일은 검증을 통과한다.")
    void success() throws IOException {
        byte[] zipBytes = createMockZip("test.png", "content".getBytes());
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", zipBytes);

        assertThatCode(() -> zipContentValidator.validateZip(file, allowedExtensions))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Zip Slip 공격 시도가 포함된 경우 예외가 발생한다.")
    void failWhenZipSlip() throws IOException {
        byte[] zipBytes = createMockZip("../../etc/passwd", "content".getBytes());
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", zipBytes);

        assertThatThrownBy(() -> zipContentValidator.validateZip(file, allowedExtensions))
                .hasMessageContaining("유효하지 않은 파일 경로");
    }

    @Test
    @DisplayName("허용되지 않은 파일 형식이 내부에 포함된 경우 예외가 발생한다.")
    void failWhenInternalFileNotAllowed() throws IOException {
        byte[] zipBytes = createMockZip("malicious.exe", "content".getBytes());
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", zipBytes);

        assertThatThrownBy(() -> zipContentValidator.validateZip(file, allowedExtensions))
                .hasMessageContaining("허용되지 않는 파일 형식");
    }

    @Test
    @DisplayName("파일 개수가 50개를 초과하면 예외가 발생한다.")
    void failWhenFileCountExceeded() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (int i = 0; i < 51; i++) {
                zos.putNextEntry(new ZipEntry("test" + i + ".png"));
                zos.write("content".getBytes());
                zos.closeEntry();
            }
        }
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", baos.toByteArray());

        assertThatThrownBy(() -> zipContentValidator.validateZip(file, allowedExtensions))
                .hasMessageContaining("파일 개수가 너무 많습니다");
    }

    private byte[] createMockZip(String entryName, byte[] content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(content);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
