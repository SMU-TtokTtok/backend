package org.project.ttokttok.domain.temp.applicant.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 임시 지원서 저장 요청
 */
public record TempApplicantSaveRequest(
        @NotBlank(message = "지원폼 ID는 필수입니다.")
        String formId,
        Map<String, Object> tempData
) {
}
