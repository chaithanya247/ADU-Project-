package com.avb.importer.service;

import com.avb.importer.model.CloseoutRecord;
import com.avb.importer.model.ImportResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DatabaseService {

    private final JdbcTemplate jdbc;

    public DatabaseService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int upsert(List<CloseoutRecord> records, ImportResult result) {
        String sql = "INSERT INTO avb_closeout " +
                "(avb_sku, closeout_id, linq_id, avb_status, closeout_type, avb_brand) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "closeout_id = VALUES(closeout_id), " +
                "linq_id = VALUES(linq_id), " +
                "avb_status = VALUES(avb_status), " +
                "closeout_type = VALUES(closeout_type), " +
                "avb_brand = VALUES(avb_brand)";

        int count = 0;
        for (CloseoutRecord rec : records) {
            try {
                jdbc.update(sql,
                        rec.getAvbSku(),
                        rec.getCloseoutId(),
                        rec.getLinqId(),
                        rec.getAvbStatus(),
                        rec.getCloseoutType(),
                        rec.getAvbBrand());
                count++;
            } catch (Exception e) {
                result.addError("DB error for SKU \"" + rec.getAvbSku() + "\": " + e.getMessage());
            }
        }
        return count;
    }
}