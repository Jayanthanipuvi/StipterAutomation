package com.BRP;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NpsLk01ExcelReportWriter {

    private static final String[] HEADERS = {
            "TestCase", "BSN", "API Status", "Database Record ID", "Validation Field",
            "Expected Value", "Actual Value", "Result", "Failure Details", "Overall Status"
    };

    public Path write(List<NpsLk01ValidationResult> results) throws Exception {
        Path directory = Path.of("target", "npslk01-reports");
        Files.createDirectories(directory);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path reportPath = directory.resolve("NPSLK01_Validation_Report_" + timestamp + ".xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream output = Files.newOutputStream(reportPath)) {
            Sheet sheet = workbook.createSheet("NPSLK01 Validation");
            CellStyle headerStyle = headerStyle(workbook);
            int rowIndex = 0;

            Row header = sheet.createRow(rowIndex++);
            for (int column = 0; column < HEADERS.length; column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(HEADERS[column]);
                cell.setCellStyle(headerStyle);
            }

            for (NpsLk01ValidationResult result : results) {
                for (NpsLk01ValidationResult.FieldValidation validation : result.validations()) {
                    Row row = sheet.createRow(rowIndex++);
                    set(row, 0, result.testCase());
                    set(row, 1, result.bsn());
                    set(row, 2, result.apiStatus() < 0 ? "" : String.valueOf(result.apiStatus()));
                    set(row, 3, result.databaseRecordId() == null ? "" : String.valueOf(result.databaseRecordId()));
                    set(row, 4, validation.field());
                    set(row, 5, validation.expected());
                    set(row, 6, validation.actual());
                    set(row, 7, validation.passed() ? "PASS" : "FAIL");
                    set(row, 8, validation.failureDetails());
                    set(row, 9, result.overallStatus());
                }
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, rowIndex - 1, 0, HEADERS.length - 1));
            for (int column = 0; column < HEADERS.length; column++) {
                sheet.autoSizeColumn(column);
            }
            workbook.write(output);
        }

        return reportPath;
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private void set(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }
}
