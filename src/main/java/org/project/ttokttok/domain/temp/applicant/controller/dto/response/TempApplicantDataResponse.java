package org.project.ttokttok.domain.temp.applicant.controller.dto.response;

import java.util.Map;
import org.project.ttokttok.domain.temp.applicant.service.dto.response.TempApplicantDataServiceResponse;

public record TempApplicantDataResponse(
        boolean hasTempData,
        Map<String, Object> data
) {
    public static TempApplicantDataResponse from(
        TempApplicantDataServiceResponse dto
    ) {
        return new TempApplicantDataResponse(
                dto.hasTempData(),
                dto.data()
        );
    }
}
