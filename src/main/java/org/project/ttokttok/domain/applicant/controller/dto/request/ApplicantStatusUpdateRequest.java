package org.project.ttokttok.domain.applicant.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;

public record ApplicantStatusUpdateRequest(
        @NotNull(message = "지원자 상태는 필수입니다.")
        PhaseStatus status
) {
}
