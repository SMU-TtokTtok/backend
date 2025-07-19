package org.project.ttokttok.domain.clubMember.service.dto.response;

public record ExcelServiceResponse(
        String clubName,
        byte[] excelData
) {
}
