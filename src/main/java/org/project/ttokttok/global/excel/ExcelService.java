package org.project.ttokttok.global.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberInExcelResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelService {

    // 부원 목록 엑셀 파일 생성
    // TODO: 메모리 이슈 시 SXSSFWorkbook 사용 고려
    public byte[] createMemberExcel(String clubName, List<ClubMemberInExcelResponse> members)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(clubName + " 부원 목록");

            createHeader(sheet);
            insertMemberData(sheet, members);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // 엑셀 헤더 생성
    private void createHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("학년");
        headerRow.createCell(1).setCellValue("이름");
        headerRow.createCell(2).setCellValue("전공");
        headerRow.createCell(3).setCellValue("역할");
    }

    // 부원 데이터 삽입
    private void insertMemberData(Sheet sheet, List<ClubMemberInExcelResponse> members) {
        for (int i = 0; i < members.size(); i++) {
            ClubMemberInExcelResponse dto = members.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(Integer.toString(dto.grade().getGrade()));
            row.createCell(1).setCellValue(dto.name());
            row.createCell(2).setCellValue(dto.major());
            row.createCell(3).setCellValue(dto.role().getMemberRoleName());
        }
    }
}