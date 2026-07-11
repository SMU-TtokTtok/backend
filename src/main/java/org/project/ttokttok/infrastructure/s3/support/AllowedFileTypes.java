package org.project.ttokttok.infrastructure.s3.support;

import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * 업로드 허용 파일 형식의 단일 소스(single source of truth).
 *
 * <p>이전에는 이미지 화이트리스트가 {@code ContentValidator}와 {@code ClubAdminService}에
 * 중복 정의되어 규칙이 어긋났다. 모든 허용 형식을 이 클래스로 중앙화하여 한 곳에서만
 * 관리한다.
 *
 * <ul>
 *   <li>{@link #IMAGES}, {@link #DOCUMENTS} — MIME 타입 기반 검증에 사용</li>
 *   <li>{@link #EXTENSIONS} — ZIP 내부 파일명(확장자) 검증에 사용</li>
 * </ul>
 */
public final class AllowedFileTypes {

    private AllowedFileTypes() {
    }

    /** 허용 이미지 MIME 타입. */
    public static final Set<String> IMAGES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",  // 아이폰 고효율 이미지
            "image/heif",  // 아이폰 고효율 이미지
            "image/gif"    // 움직이는 이미지
    );

    /** 허용 문서 MIME 타입. */
    public static final Set<String> DOCUMENTS = Set.of(
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

    /**
     * ZIP 내부 파일명 검증에 사용하는 허용 확장자(소문자).
     *
     * <p>MIME 타입 집합과 달리 파일명은 확장자만 노출되므로, 확장자 전용 화이트리스트로
     * 정확히 일치 검사한다. (예: {@code report.docx} → {@code docx})
     */
    public static final Set<String> EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "heic", "heif", "gif",
            "pdf", "doc", "docx", "hwp", "hwpx", "hwpml",
            "ppt", "pptx", "xls", "xlsx", "csv", "txt", "zip"
    );

    /** 이미지·문서를 모두 포함한 허용 MIME 타입 전체. */
    public static Set<String> allMimeTypes() {
        return Stream.concat(IMAGES.stream(), DOCUMENTS.stream())
                .collect(Collectors.toUnmodifiableSet());
    }
}
