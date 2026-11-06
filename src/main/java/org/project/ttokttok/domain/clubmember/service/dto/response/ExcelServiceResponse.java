package org.project.ttokttok.domain.clubmember.service.dto.response;

public record ExcelServiceResponse(
        String clubName,
        byte[] excelData
) {
}
