package org.project.ttokttok.global.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberInExcelResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExcelService - 부원 목록 엑셀 생성")
class ExcelServiceTest {

    private final ExcelService excelService = new ExcelService();

    @Test
    @DisplayName("부원 목록으로 유효한 XLSX 바이트 배열을 생성한다")
    void createMemberExcel_returnsValidXlsxBytes() throws IOException {
        // given
        List<ClubMemberInExcelResponse> members = List.of(
                new ClubMemberInExcelResponse(Grade.FIRST_GRADE, "홍길동", "컴퓨터과학과", MemberRole.PRESIDENT),
                new ClubMemberInExcelResponse(Grade.THIRD_GRADE, "김철수", "소프트웨어학과", MemberRole.MEMBER)
        );

        // when
        byte[] result = excelService.createMemberExcel("떡떡 동아리", members);

        // then
        assertThat(result).isNotEmpty();
        // XLSX(zip) 시그니처: 'P'(0x50), 'K'(0x4B)
        assertThat(result[0]).isEqualTo((byte) 0x50);
        assertThat(result[1]).isEqualTo((byte) 0x4B);
    }

    @Test
    @DisplayName("생성된 엑셀의 시트명/헤더/데이터 행이 올바르게 채워진다")
    void createMemberExcel_hasCorrectSheetHeaderAndRows() throws IOException {
        // given
        List<ClubMemberInExcelResponse> members = List.of(
                new ClubMemberInExcelResponse(Grade.SECOND_GRADE, "이영희", "경영학과", MemberRole.EXECUTIVE)
        );

        // when
        byte[] result = excelService.createMemberExcel("테스트동아리", members);

        // then
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("테스트동아리 부원 목록");

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("학년");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("이름");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("전공");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("역할");

            Row dataRow = sheet.getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("2");
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("이영희");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("경영학과");
            assertThat(dataRow.getCell(3).getStringCellValue()).isEqualTo("임원진");
        }
    }

    @Test
    @DisplayName("부원이 없어도 헤더만 있는 엑셀을 생성한다")
    void createMemberExcel_withEmptyMembers_createsHeaderOnly() throws IOException {
        // when
        byte[] result = excelService.createMemberExcel("빈동아리", List.of());

        // then
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(1)).isNull();
        }
    }
}
