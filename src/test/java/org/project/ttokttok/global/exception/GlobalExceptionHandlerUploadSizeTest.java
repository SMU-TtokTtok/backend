package org.project.ttokttok.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 업로드 용량 초과 응답을 검증한다.
 *
 * QA 항목 "허용되지 않은 파일 형식/용량 초과 시 안내가 표시되는가" 중 용량 초과 경로다.
 * 형식 검증은 서비스 계층에서 이미 다루므로 여기서는 용량만 본다.
 */
class GlobalExceptionHandlerUploadSizeTest {

    private static final long MAX_UPLOAD_BYTES = 20L * 1024 * 1024;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("업로드 용량을 초과하면 413과 안내 문구를 반환한다")
    void handleMaxSize_returnsPayloadTooLarge() {
        // given
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(MAX_UPLOAD_BYTES);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleMaxSize(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(response.getBody().details()).isEqualTo("업로드 가능한 최대 용량을 초과했습니다.");
    }
}
