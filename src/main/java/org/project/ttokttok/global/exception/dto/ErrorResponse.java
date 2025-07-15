package org.project.ttokttok.global.exception.dto;

import lombok.Builder;

@Builder
public record ErrorResponse<T>(
        int statusCode,
        T details
) {
}
