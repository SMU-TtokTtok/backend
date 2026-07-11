package org.project.ttokttok.infrastructure.s3.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 업로드 검증 계약.
 *
 * <p>검증은 비용이 낮은 것부터 높은 것 순서로 수행하도록 메서드를 분리했다.
 * ({@link #validateNotEmpty} → {@link #validateSize} → {@link #validateType} →
 * {@link #validateFileName} → {@link #validateArchive}) 무거운 ZIP 스트리밍 검증
 * ({@link #validateArchive})은 값싼 검증을 모두 통과한 뒤에만 실행된다.
 */
public interface ContentValidatable {

    /** 파일이 null이거나 비어있지 않은지 확인한다. (가장 값싼 fail-fast 가드) */
    void validateNotEmpty(MultipartFile content);

    void validateSize(long size);

    void validateType(String type);

    void validateFileName(String fileName);

    /** ZIP 등 아카이브 파일의 내부를 스트리밍 검증한다. 일반 파일은 아무 동작도 하지 않는다. */
    void validateArchive(MultipartFile content);
}
