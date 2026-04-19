package com.avb.importer.service;

import com.avb.importer.model.CloseoutRecord;
import com.avb.importer.model.ImportResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ExcelParserService {

    private static final Map<String, String> COLUMN_MAP = new LinkedHashMap<>();

    static {
        COLUMN_MAP.put("SKU", "avb_sku");
        COLUMN_MAP.put("Brand", "avb_brand");
        COLUMN_MAP.put("Product Linq ID", "linq_id");
        COLUMN_MAP.put("CLOSEOUT_ID", "closeout_id");
        COLUMN_MAP.put("CLOSEOUT_TYPE", "closeout_type");
        COLUMN_MAP.put("Active Inactive Status-Default", "avb_status");
    }

    public ParsedResult parse(InputStream is) throws Exception {
        List<CloseoutRecord> records = new ArrayList<>();
        ImportResult result = new ImportResult();

        try (Workbook wb = new XSSFWorkbook(is)) {
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                parseSheet(wb.getSheetAt(s), records, result);
            }
        }
        return new ParsedResult(records, result);
    }

    private void parseSheet(Sheet sheet, List<CloseoutRecord> records, ImportResult result) {
        String sheetName = sheet.getSheetName();
        List<CellRangeAddress> merges = sheet.getMergedRegions();

        HeaderSearchResult headerSearch = findHeaders(sheet, merges);
        if (headerSearch.match == null) {
            result.addWarning("Sheet \"" + sheetName + "\" skipped - missing columns: "
                    + String.join(", ", headerSearch.missingColumns));
            return;
        }

        Map<Integer, String> colIndexToName = headerSearch.match.headers;
        Map<String, Integer> fieldToCol = new HashMap<>();
        for (Map.Entry<String, String> entry : COLUMN_MAP.entrySet()) {
            for (Map.Entry<Integer, String> hEntry : colIndexToName.entrySet()) {
                if (hEntry.getValue().equalsIgnoreCase(entry.getKey().trim())) {
                    fieldToCol.put(entry.getValue(), hEntry.getKey());
                    break;
                }
            }
        }

        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> entry : COLUMN_MAP.entrySet()) {
            if (!fieldToCol.containsKey(entry.getValue())) {
                missing.add(entry.getKey());
            }
        }
        if (!missing.isEmpty()) {
            result.addWarning("Sheet \"" + sheetName + "\" skipped - missing columns: "
                    + String.join(", ", missing));
            return;
        }

        int dataStartRow = findDataStartRow(sheet, headerSearch.match.lastHeaderRowIndex + 1,
                fieldToCol.get("avb_sku"));
        DataFormatter formatter = new DataFormatter();

        for (int rowIdx = dataStartRow; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;
            CloseoutRecord rec = buildRecord(row, rowIdx + 1, sheetName,
                    fieldToCol, formatter, result);
            if (rec != null) records.add(rec);
        }
    }

    private HeaderSearchResult findHeaders(Sheet sheet, List<CellRangeAddress> merges) {
        int maxScan = sheet.getLastRowNum() + 1;
        Set<String> required = new HashSet<>();
        for (String header : COLUMN_MAP.keySet()) {
            required.add(header.trim().toLowerCase());
        }

        List<Map<Integer, String>> rawRows = new ArrayList<>();
        for (int r = 0; r < maxScan; r++) {
            rawRows.add(resolveRowHeaders(sheet.getRow(r), merges));
        }

        Map<Integer, String> bestCandidate = Collections.emptyMap();
        int bestMatchCount = -1;

        for (int r = 0; r < rawRows.size(); r++) {
            Map<Integer, String> single = rawRows.get(r);
            int singleMatchCount = countMatches(single, required);
            if (singleMatchCount > bestMatchCount) {
                bestMatchCount = singleMatchCount;
                bestCandidate = single;
            }
            if (containsAll(single, required)) {
                return new HeaderSearchResult(new HeaderMatch(r, single), Collections.emptyList());
            }

            if (r + 1 < rawRows.size()) {
                Map<Integer, String> combined = new HashMap<>(single);
                combined.putAll(rawRows.get(r + 1));
                int combinedMatchCount = countMatches(combined, required);
                if (combinedMatchCount > bestMatchCount) {
                    bestMatchCount = combinedMatchCount;
                    bestCandidate = combined;
                }
                if (containsAll(combined, required)) {
                    return new HeaderSearchResult(new HeaderMatch(r + 1, combined), Collections.emptyList());
                }
            }
        }

        return new HeaderSearchResult(null, missingColumns(bestCandidate));
    }

    private Map<Integer, String> resolveRowHeaders(Row row, List<CellRangeAddress> merges) {
        Map<Integer, String> map = new HashMap<>();
        if (row == null) return map;
        DataFormatter fmt = new DataFormatter();

        for (Cell cell : row) {
            String val = fmt.formatCellValue(cell).trim();
            if (!val.isEmpty()) {
                int col = cell.getColumnIndex();
                map.put(col, val);
                for (CellRangeAddress merge : merges) {
                    if (merge.getFirstRow() == row.getRowNum()
                            && merge.getFirstColumn() == col) {
                        for (int c = merge.getFirstColumn(); c <= merge.getLastColumn(); c++) {
                            map.put(c, val);
                        }
                    }
                }
            }
        }
        return map;
    }

    private boolean containsAll(Map<Integer, String> headers, Set<String> required) {
        Set<String> found = new HashSet<>();
        for (String value : headers.values()) {
            found.add(value.trim().toLowerCase());
        }
        return found.containsAll(required);
    }

    private int countMatches(Map<Integer, String> headers, Set<String> required) {
        Set<String> found = new HashSet<>();
        for (String value : headers.values()) {
            found.add(value.trim().toLowerCase());
        }

        int matches = 0;
        for (String requiredValue : required) {
            if (found.contains(requiredValue)) {
                matches++;
            }
        }
        return matches;
    }

    private List<String> missingColumns(Map<Integer, String> headers) {
        Set<String> found = new HashSet<>();
        for (String value : headers.values()) {
            found.add(value.trim().toLowerCase());
        }

        List<String> missing = new ArrayList<>();
        for (String header : COLUMN_MAP.keySet()) {
            if (!found.contains(header.trim().toLowerCase())) {
                missing.add(header);
            }
        }
        return missing;
    }

    private int findDataStartRow(Sheet sheet, int startRow, int skuColIdx) {
        Set<String> metaValues = new HashSet<>(Arrays.asList(
                "required", "read only", "update", "string", "integer", "number", "boolean", ""));
        DataFormatter fmt = new DataFormatter();
        int start = startRow;

        for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                start = r + 1;
                continue;
            }

            String val = getString(row, skuColIdx, fmt).toLowerCase();
            if (metaValues.contains(val)) {
                start = r + 1;
            } else {
                break;
            }
        }
        return start;
    }

    private CloseoutRecord buildRecord(Row row, int displayRow, String sheetName,
                                       Map<String, Integer> fieldToCol,
                                       DataFormatter fmt, ImportResult result) {
        String avbSku = getString(row, fieldToCol.get("avb_sku"), fmt);
        String closeoutId = getString(row, fieldToCol.get("closeout_id"), fmt);
        String linqRaw = getString(row, fieldToCol.get("linq_id"), fmt);
        String avbStatus = getString(row, fieldToCol.get("avb_status"), fmt);
        String closeoutTy = getString(row, fieldToCol.get("closeout_type"), fmt);
        String avbBrand = getString(row, fieldToCol.get("avb_brand"), fmt);

        if (avbSku.isEmpty() && closeoutId.isEmpty() && linqRaw.isEmpty()) return null;

        boolean valid = true;
        valid &= requireValue(avbSku, displayRow, colLetter(fieldToCol.get("avb_sku")),
                "SKU", sheetName, result);
        valid &= requireValue(closeoutId, displayRow, colLetter(fieldToCol.get("closeout_id")),
                "CLOSEOUT_ID", sheetName, result);
        valid &= requireValue(linqRaw, displayRow, colLetter(fieldToCol.get("linq_id")),
                "Product Linq ID", sheetName, result);
        valid &= requireValue(avbStatus, displayRow, colLetter(fieldToCol.get("avb_status")),
                "Active Inactive Status-Default", sheetName, result);
        valid &= requireValue(closeoutTy, displayRow, colLetter(fieldToCol.get("closeout_type")),
                "CLOSEOUT_TYPE", sheetName, result);
        valid &= requireValue(avbBrand, displayRow, colLetter(fieldToCol.get("avb_brand")),
                "Brand", sheetName, result);
        if (!valid) return null;

        int linqId;
        try {
            String cleaned = linqRaw.replace(",", "").trim();
            if (cleaned.contains(".")) {
                cleaned = cleaned.substring(0, cleaned.indexOf('.'));
            }
            linqId = Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            result.addError("Sheet \"" + sheetName + "\" row " + displayRow
                    + " col " + colLetter(fieldToCol.get("linq_id"))
                    + ": Cannot parse \"" + linqRaw + "\" as integer");
            return null;
        }

        return new CloseoutRecord(avbSku, closeoutId, linqId, avbStatus, closeoutTy, avbBrand);
    }

    private boolean requireValue(String value, int row, String col, String fieldName,
                                 String sheetName, ImportResult result) {
        if (value == null || value.isEmpty()) {
            result.addWarning("Sheet \"" + sheetName + "\" row " + row + " col " + col
                    + ": Missing value for \"" + fieldName + "\" - row skipped");
            return false;
        }
        return true;
    }

    private String getString(Row row, Integer colIdx, DataFormatter fmt) {
        if (colIdx == null || row == null) return "";
        Cell cell = row.getCell(colIdx);
        return cell == null ? "" : fmt.formatCellValue(cell).trim();
    }

    private String colLetter(int colIdx) {
        StringBuilder sb = new StringBuilder();
        int col = colIdx;
        while (col >= 0) {
            sb.insert(0, (char) ('A' + col % 26));
            col = col / 26 - 1;
        }
        return sb.toString();
    }

    private static class HeaderMatch {
        private final int lastHeaderRowIndex;
        private final Map<Integer, String> headers;

        private HeaderMatch(int lastHeaderRowIndex, Map<Integer, String> headers) {
            this.lastHeaderRowIndex = lastHeaderRowIndex;
            this.headers = headers;
        }
    }

    private static class HeaderSearchResult {
        private final HeaderMatch match;
        private final List<String> missingColumns;

        private HeaderSearchResult(HeaderMatch match, List<String> missingColumns) {
            this.match = match;
            this.missingColumns = missingColumns;
        }
    }

    public static class ParsedResult {
        private final List<CloseoutRecord> records;
        private final ImportResult parseResult;

        public ParsedResult(List<CloseoutRecord> records, ImportResult parseResult) {
            this.records = records;
            this.parseResult = parseResult;
        }

        public List<CloseoutRecord> getRecords() { return records; }

        public ImportResult getParseResult() { return parseResult; }
    }
}
