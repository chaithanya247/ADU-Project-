package com.avb.importer.service;

import com.avb.importer.model.ImportResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportService {

    private final ExcelParserService parser;
    private final DatabaseService    db;

    public ImportService(ExcelParserService parser, DatabaseService db) {
        this.parser = parser;
        this.db     = db;
    }

    public ImportResult importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ImportResult.error("No file provided.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            return ImportResult.error("Only .xlsx files are accepted.");
        }

        ExcelParserService.ParsedResult parsed;
        try {
            parsed = parser.parse(file.getInputStream());
        } catch (Exception e) {
            return ImportResult.error("Failed to parse Excel file: " + e.getMessage());
        }

        ImportResult result = parsed.getParseResult();

        if (parsed.getRecords().isEmpty()) {
            if (!result.hasErrors() && !result.hasWarnings()) {
                result.addError("No importable records found in the file.");
            }
            result.finalizeStatus();
            return result;
        }

        int count = db.upsert(parsed.getRecords(), result);
        result.setImportedCount(count);
        result.finalizeStatus();
        return result;
    }
}