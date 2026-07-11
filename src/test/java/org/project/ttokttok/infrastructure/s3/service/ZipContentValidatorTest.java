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
    private final Set<String> allowedExtensions = Set.of("png", "pdf", "docx");

    @Test
    @DisplayName("정상적인 ZIP 파일은 검증을 통과한다.")
    void success() throws IOException {
        byte[] zipBytes = createMockZip("test.png", "content".getBytes());
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", zipBytes);

        assertThatCode(() -> zipContentValidator.validateZip(file, allowedExtensions))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("하위 폴더에 든 파일(folder/file)은 Zip Slip으로 오탐하지 않고 통과한다.")
    void successWhenNestedDirectory() throws IOException {
        byte[] zipBytes = createMockZip("docs/report.pdf", "content".getBytes());
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", zipBytes);

        assertThatCode(() -> zipContentValidator.validateZip(file, allowedExtensions))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MIME 타입이 아닌 실제 확장자(docx)로 정확히 매칭하여 통과한다.")
    void successWhenExtensionExactMatch() throws IOException {
        byte[] zipBytes = createMockZip("resume.docx", "content".getBytes());
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

    @Test
    @DisplayName("압축 해제 실제 용량이 한계를 초과하면 예외가 발생한다. (헤더값이 아닌 실측 기준)")
    void failWhenUncompressedSizeExceeded() throws IOException {
        // 압축 해제 한계를 10바이트로 낮춘 검증기 (실측 누적 로직 검증)
        ZipContentValidator smallLimitValidator = new ZipContentValidator(50, 10L);
        byte[] zipBytes = createMockZip("big.png", new byte[1024]); // 실제 1KB 내용
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", zipBytes);

        assertThatThrownBy(() -> smallLimitValidator.validateZip(file, allowedExtensions))
                .hasMessageContaining("압축 해제 시 허용 용량을 초과");
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
