package org.project.ttokttok.domain.applyform.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApplyFormStatus {
    // 지원 폼 활성화 여부를 명시하는 열거형
    ACTIVE("활성화"),
    INACTIVE("비활성화");

    final String status;
}
