package org.project.ttokttok.domain.user.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(   // null 값은 JSON 에서 제외
        boolean success,        // 성공 여부
        String message,         // 응답 메시지
        T data                  // 실제 데이터 (Generic)
) {
    // 성공 응답 (데이터 있음)
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // 성공 응답 (데이터 없음)
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(null)
                .build();
    }

    // 실패 응답
    public static <T> ApiResponse<T> failure(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
