package org.project.ttokttok.infrastructure.s3.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
public class ContentValidator implements ContentValidatable {

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private static final Set<String> ALLOWED_DOCS_TYPES =
            Set.of(
                    "application/pdf",  // PDF
                    "application/msword",  // Word (doc)
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",  // Word (docx)
                    "application/x-hwp",  // 한글 (hwp)
                    "application/vnd.hancom.hwp",  // 한글 (hwp, 일부 환경)
                    "application/vnd.hancom.hwpx", // 한글 (hwpx, 신형 포맷)
                    "application/x-hwpml" // 한글 (hwpml, 마이너)
            );

    private static final long MAX_CONTENT_SIZE = 5 * 1024 * 1024L; // 5MB
    private static final String FILE_NAME_REGEX = ".*[\\\\/:*?\"<>|].*";
    private static final int MAX_FILE_NAME_LENGTH = 255; // 파일 이름 최대 길이

    @Override
    public void validateContent(MultipartFile content) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있거나 존재하지 않습니다.");
        }
    }

    @Override
    public void validateSize(long size) {
        if (size > MAX_CONTENT_SIZE) {
            throw new IllegalArgumentException("파일 크기가 5MB를 초과할 수 없습니다.");
        }
    }

    @Override
    public void validateType(String type) {
        if (!ALLOWED_IMAGE_TYPES.contains(type) && !ALLOWED_DOCS_TYPES.contains(type)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. 이미지 또는 문서 파일만 허용됩니다.");
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
