package org.project.ttokttok.infrastructure.s3.service;

import org.project.ttokttok.infrastructure.s3.exception.S3FileMaxSizeOverException;
import org.project.ttokttok.infrastructure.s3.exception.UnsupportedFileTypeException;
import org.project.ttokttok.infrastructure.s3.support.AllowedFileTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ContentValidator implements ContentValidatable {

    private final ZipContentValidator zipContentValidator;

    // 애플리케이션 레벨 사이즈 상한은 서블릿 컨테이너 한계(spring.servlet.multipart.max-file-size)와
    // 동일한 yml 값을 단일 소스로 주입받아, 프로파일별로 두 한계가 어긋나지 않도록 한다.
    private final long maxContentSize;

    private static final String FILE_NAME_REGEX = ".*[\\\\/:*?\"<>|].*";
    private static final int MAX_FILE_NAME_LENGTH = 255; // 파일 이름 최대 길이

    private static final String ZIP_MIME = "application/zip";
    private static final String ZIP_MIME_WINDOWS = "application/x-zip-compressed";

    public ContentValidator(ZipContentValidator zipContentValidator,
                            @Value("${spring.servlet.multipart.max-file-size:20MB}") DataSize maxFileSize) {
        this.zipContentValidator = zipContentValidator;
        this.maxContentSize = maxFileSize.toBytes();
    }

    @Override
    public void validateNotEmpty(MultipartFile content) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있거나 존재하지 않습니다.");
        }
    }

    @Override
    public void validateSize(long size) {
        if (size > maxContentSize) {
            throw new S3FileMaxSizeOverException();
        }
    }

    @Override
    public void validateType(String type) {
        if (!AllowedFileTypes.IMAGES.contains(type) && !AllowedFileTypes.DOCUMENTS.contains(type)) {
            throw new UnsupportedFileTypeException();
        }
    }

    @Override
    public void validateFileName(String fileName) {
        validateFileNameEmpty(fileName);

        validateFileNameTooLong(fileName);

        validateChars(fileName);
    }

    @Override
    public void validateArchive(MultipartFile content) {
        // ZIP 파일인 경우에만 내부를 스트리밍 검증한다. (값싼 검증을 모두 통과한 뒤 수행)
        String contentType = content.getContentType();
        if (isZip(contentType)) {
            zipContentValidator.validateZip(content, AllowedFileTypes.EXTENSIONS);
        }
    }

    private boolean isZip(String contentType) {
        return ZIP_MIME.equals(contentType) || ZIP_MIME_WINDOWS.equals(contentType);
    }

    // 파일 이름이 비어있지 않은지 확인
    private void validateFileNameEmpty(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("파일 이름이 비어있습니다.");
        }
    }

    // 유효하지 않은 특수 문자 확인
    private void validateChars(String fileName) {
        if (fileName.matches(FILE_NAME_REGEX)) {
            throw new IllegalArgumentException("파일 이름에 허용되지 않는 특수 문자가 포함되어 있습니다.");
        }
    }

    // 파일 이름이 너무 길지 않은지 확인
    private void validateFileNameTooLong(String fileName) {
        if (fileName.length() > MAX_FILE_NAME_LENGTH) {
            throw new IllegalArgumentException("파일 이름이 너무 깁니다. (최대 255자)");
        }
    }
}
