package org.project.ttokttok.global.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenProperties {
    AUTH_HEADER("Authorization"),
    BEARER_PREFIX("Bearer");

    final String value;
}
