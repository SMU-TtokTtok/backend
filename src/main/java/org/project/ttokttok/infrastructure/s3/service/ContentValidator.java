package org.project.ttokttok.infrastructure.s3.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.infrastructure.s3.exception.S3FileMaxSizeOverException;
import org.project.ttokttok.infrastructure.s3.exception.UnsupportedFileTypeException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContentValidator implements ContentValidatable {

    private final ZipContentValidator zipContentValidator;

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg",
                    "image/png",
                    "image/webp",
                    "image/heic",  // 아이폰 고효율 이미지
                    "image/heif",  // 아이폰 고효율 이미지
                    "image/gif"   // 움직이는 이미지
            );

    private static final Set<String> ALLOWED_DOCS_TYPES =
            Set.of(
                    "application/pdf",  // PDF
                    "application/msword",  // Word (doc)
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",  // Word (docx)
                    "application/x-hwp",  // 한글 (hwp)
                    "application/vnd.hancom.hwp",  // 한글 (hwp, 일부 환경)
                    "application/vnd.hancom.hwpx", // 한글 (hwpx, 신형 포맷)
                    "application/x-hwpml", // 한글 (hwpml, 마이너)
                    "application/vnd.ms-powerpoint", // PPT
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation", // PPTX
                    "application/vnd.ms-excel", // XLS
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // XLSX
                    "text/csv", // CSV
                    "text/plain", // TXT
                    "application/zip", // ZIP
                    "application/x-zip-compressed" // ZIP (Windows 등)
            );

    private static final long MAX_CONTENT_SIZE = 20 * 1024 * 1024L; // 20MB
    private static final String FILE_NAME_REGEX = ".*[\\\\/:*?\"<>|].*";
    private static final int MAX_FILE_NAME_LENGTH = 255; // 파일 이름 최대 길이

    @Override
    public void validateContent(MultipartFile content) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있거나 존재하지 않습니다.");
        }

        // ZIP 파일인 경우 내부 메타데이터 스트리밍 검증 수행
        String contentType = content.getContentType();
        if (contentType != null && (contentType.equals("application/zip") || contentType.equals("application/x-zip-compressed"))) {
            Set<String> allAllowedTypes = new HashSet<>(ALLOWED_IMAGE_TYPES);
            allAllowedTypes.addAll(ALLOWED_DOCS_TYPES);
            zipContentValidator.validateZip(content, allAllowedTypes);
        }
    }

    @Override
    public void validateSize(long size) {
        if (size > MAX_CONTENT_SIZE) {
            throw new S3FileMaxSizeOverException();
        }
    }

    @Override
    public void validateType(String type) {
        if (!ALLOWED_IMAGE_TYPES.contains(type) && !ALLOWED_DOCS_TYPES.contains(type)) {
            throw new UnsupportedFileTypeException();
        }
    }

    @Override
    public void validateFileName(String fileName) {
        validateFileNameEmpty(fileName);

        validateFileNameTooLong(fileName);

        validateChars(fileName);
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
