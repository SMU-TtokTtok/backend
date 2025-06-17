package org.project.ttokttok.global.error.dto;

import lombok.Builder;

@Builder
public record ErrorResponse<T>(
        int statusCode,
        T details
) {
}
