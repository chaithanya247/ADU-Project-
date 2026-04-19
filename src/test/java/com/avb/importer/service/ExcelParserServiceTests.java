package com.avb.importer.service;

import com.avb.importer.model.CloseoutRecord;
import com.avb.importer.model.ImportResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelParserServiceTests {

    private final ExcelParserService parser = new ExcelParserService();

    @Test
    void parsesWorkbookWhenHeaderRowIsDeepAndColumnsAreOutOfOrder() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet invalidSheet = workbook.createSheet("Invalid");
            Row invalidHeader = invalidSheet.createRow(0);
            invalidHeader.createCell(0).setCellValue("SKU");
            invalidHeader.createCell(1).setCellValue("Brand");

            Sheet validSheet = workbook.createSheet("Valid");
            for (int i = 0; i < 25; i++) {
                validSheet.createRow(i).createCell(0).setCellValue("notes");
            }

            Row header = validSheet.createRow(25);
            header.createCell(0).setCellValue("Brand");
            header.createCell(1).setCellValue("CLOSEOUT_TYPE");
            header.createCell(2).setCellValue("Active Inactive Status-Default");
            header.createCell(3).setCellValue("SKU");
            header.createCell(4).setCellValue("Product Linq ID");
            header.createCell(5).setCellValue("CLOSEOUT_ID");

            Row metadata = validSheet.createRow(26);
            metadata.createCell(3).setCellValue("required");

            Row data = validSheet.createRow(27);
            data.createCell(0).setCellValue("  ACME  ");
            data.createCell(1).setCellValue("  Clearance ");
            data.createCell(2).setCellValue(" Active ");
            data.createCell(3).setCellValue("  SKU-1 ");
            data.createCell(4).setCellValue("1,234");
            data.createCell(5).setCellValue("  CO-9 ");

            ExcelParserService.ParsedResult parsed = parser.parse(asStream(workbook));

            List<CloseoutRecord> records = parsed.getRecords();
            ImportResult result = parsed.getParseResult();

            assertEquals(1, records.size());
            assertEquals("SKU-1", records.get(0).getAvbSku());
            assertEquals("ACME", records.get(0).getAvbBrand());
            assertEquals("Clearance", records.get(0).getCloseoutType());
            assertEquals("Active", records.get(0).getAvbStatus());
            assertEquals("CO-9", records.get(0).getCloseoutId());
            assertEquals(1234, records.get(0).getLinqId());
            assertTrue(result.getWarnings().stream()
                    .anyMatch(msg -> msg.contains("Sheet \"Invalid\" skipped - missing columns")));
        }
    }

    @Test
    void reportsMissingColumnsWhenNoCompleteHeaderExists() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("MissingHeaders");
            Row header = sheet.createRow(3);
            header.createCell(0).setCellValue("SKU");
            header.createCell(1).setCellValue("Brand");
            header.createCell(2).setCellValue("Ignored");

            ExcelParserService.ParsedResult parsed = parser.parse(asStream(workbook));

            assertTrue(parsed.getRecords().isEmpty());
            assertTrue(parsed.getParseResult().getWarnings().stream().anyMatch(msg ->
                    msg.contains("MissingHeaders")
                            && msg.contains("Product Linq ID")
                            && msg.contains("CLOSEOUT_ID")
                            && msg.contains("CLOSEOUT_TYPE")
                            && msg.contains("Active Inactive Status-Default")));
        }
    }

    private ByteArrayInputStream asStream(XSSFWorkbook workbook) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}
