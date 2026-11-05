package org.project.ttokttok.domain.temp.applicant.service.dto.response;

import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record TempApplicantDataServiceResponse(
        boolean hasTempData,
        Map<String, Object> data
) {
}
