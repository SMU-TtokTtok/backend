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

    private static final int MAX_FILE_COUNT = 50;
    private static final long MAX_UNCOMPRESSED_SIZE = 100 * 1024 * 1024L; // 100MB

    public void validateZip(MultipartFile file, Set<String> allowedExtensions) {
        int fileCount = 0;
        long totalUncompressedSize = 0;

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                // 1. Zip Slip 방지 (경로 조작)
                validateZipSlip(name);

                // 2. 디렉토리는 건너뛰고 파일만 검사
                if (!entry.isDirectory()) {
                    fileCount++;
                    
                    // 3. 파일 개수 제한
                    validateFileCount(fileCount);

                    // 4. 내부 파일 확장자 검사
                    validateInternalExtension(name, allowedExtensions);

                    // 5. 압축 해제 후 용량 합산
                    long size = entry.getSize();
                    if (size > 0) {
                        totalUncompressedSize += size;
                    }
                    
                    // 6. 압축 해제 총 용량 제한
                    validateTotalUncompressedSize(totalUncompressedSize);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("ZIP 파일 검증 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorMessage.S3_FILE_UPLOAD_ERROR) {};
        }
    }

    private void validateZipSlip(String name) {
        if (name.contains("..") || name.contains("/") || name.contains("\\")) {
            throw new CustomException(ErrorMessage.S3_ZIP_SLIP_ERROR) {};
        }
    }

    private void validateFileCount(int count) {
        if (count > MAX_FILE_COUNT) {
            throw new CustomException(ErrorMessage.S3_ZIP_FILE_COUNT_LIMIT) {};
        }
    }

    private void validateTotalUncompressedSize(long size) {
        if (size > MAX_UNCOMPRESSED_SIZE) {
            throw new CustomException(ErrorMessage.S3_ZIP_UNCOMPRESSED_SIZE_LIMIT) {};
        }
    }

    private void validateInternalExtension(String name, Set<String> allowedExtensions) {
        String extension = getExtension(name).toLowerCase();
        // 간단한 확장자 매칭 (MIME 타입 기반이 아닌 파일명 기반)
        boolean isAllowed = allowedExtensions.stream()
                .anyMatch(allowed -> allowed.contains(extension));
        
        if (!isAllowed) {
            throw new CustomException(ErrorMessage.S3_ZIP_INTERNAL_FILE_TYPE_ERROR) {};
        }
    }

    private String getExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) return "";
        return fileName.substring(lastIndex + 1);
    }
}
