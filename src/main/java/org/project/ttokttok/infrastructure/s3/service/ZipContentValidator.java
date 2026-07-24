package org.project.ttokttok.infrastructure.s3.service;

import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.global.exception.exception.CustomException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
public class ZipContentValidator {

    private static final int DEFAULT_MAX_FILE_COUNT = 50;
    private static final long DEFAULT_MAX_UNCOMPRESSED_SIZE = 100 * 1024 * 1024L; // 100MB
    private static final int BUFFER_SIZE = 8192;

    private final int maxFileCount;
    private final long maxUncompressedSize;

    public ZipContentValidator() {
        this(DEFAULT_MAX_FILE_COUNT, DEFAULT_MAX_UNCOMPRESSED_SIZE);
    }

    // 테스트에서 낮은 한계로 검증할 수 있도록 한계를 주입 가능하게 둔다.
    ZipContentValidator(int maxFileCount, long maxUncompressedSize) {
        this.maxFileCount = maxFileCount;
        this.maxUncompressedSize = maxUncompressedSize;
    }

    public void validateZip(MultipartFile file, Set<String> allowedExtensions) {
        int fileCount = 0;
        long totalUncompressedSize = 0;

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                // 1. Zip Slip 방지 (상위 경로 탈출/절대 경로만 차단, 정상 하위 폴더는 허용)
                validateZipSlip(name);

                // 2. 디렉토리는 건너뛰고 파일만 검사
                if (!entry.isDirectory()) {
                    fileCount++;

                    // 3. 파일 개수 제한
                    validateFileCount(fileCount);

                    // 4. 내부 파일 확장자 검사
                    validateInternalExtension(name, allowedExtensions);

                    // 5. 압축 해제 스트림을 실제로 읽으며 누적 (헤더값 신뢰 없이 zip bomb 방어)
                    totalUncompressedSize += readAndMeasure(zis, totalUncompressedSize);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("ZIP 파일 검증 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorMessage.S3_FILE_UPLOAD_ERROR) {};
        }
    }

    /**
     * 현재 엔트리의 압축 해제 스트림을 끝까지 읽으며 실제 바이트 수를 측정한다.
     * 누적 총량이 한계를 넘는 즉시 예외를 던져 조작된 헤더 크기나 zip bomb을 방어한다.
     *
     * @return 이 엔트리의 실제 압축 해제 바이트 수
     */
    private long readAndMeasure(ZipInputStream zis, long alreadyCounted) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long entrySize = 0;
        int read;
        while ((read = zis.read(buffer)) != -1) {
            entrySize += read;
            validateTotalUncompressedSize(alreadyCounted + entrySize);
        }
        return entrySize;
    }

    private void validateZipSlip(String name) {
        // 상위 경로 탈출("..")과 절대 경로만 차단한다. "folder/file.pdf" 같은 정상 하위 경로는 허용.
        if (name.contains("..") || name.startsWith("/") || name.startsWith("\\")) {
            throw new CustomException(ErrorMessage.S3_ZIP_SLIP_ERROR) {};
        }
    }

    private void validateFileCount(int count) {
        if (count > maxFileCount) {
            throw new CustomException(ErrorMessage.S3_ZIP_FILE_COUNT_LIMIT) {};
        }
    }

    private void validateTotalUncompressedSize(long size) {
        if (size > maxUncompressedSize) {
            throw new CustomException(ErrorMessage.S3_ZIP_UNCOMPRESSED_SIZE_LIMIT) {};
        }
    }

    private void validateInternalExtension(String name, Set<String> allowedExtensions) {
        String extension = getExtension(name);
        // 확장자 전용 화이트리스트에 대한 정확 일치 검사 (파일명 기반)
        if (!allowedExtensions.contains(extension)) {
            throw new CustomException(ErrorMessage.S3_ZIP_INTERNAL_FILE_TYPE_ERROR) {};
        }
    }

    private String getExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return "";
        }
        return fileName.substring(lastIndex + 1).toLowerCase();
    }
}
