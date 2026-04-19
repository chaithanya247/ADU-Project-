package com.avb.importer.model;

import java.util.ArrayList;
import java.util.List;

public class ImportResult {

    public enum Status { SUCCESS, PARTIAL, ERROR }

    private Status status;
    private int importedCount;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors   = new ArrayList<>();

    public ImportResult() {}

    public static ImportResult error(String message) {
        ImportResult r = new ImportResult();
        r.status = Status.ERROR;
        r.errors.add(message);
        return r;
    }

    public void addWarning(String msg) { warnings.add(msg); }
    public void addError(String msg)   { errors.add(msg); }

    public void finalizeStatus() {
        if (!errors.isEmpty() && importedCount == 0) {
            status = Status.ERROR;
        } else if (!warnings.isEmpty() || !errors.isEmpty()) {
            status = Status.PARTIAL;
        } else {
            status = Status.SUCCESS;
        }
    }

    public Status getStatus()          { return status; }
    public int getImportedCount()      { return importedCount; }
    public List<String> getWarnings()  { return warnings; }
    public List<String> getErrors()    { return errors; }
    public boolean hasWarnings()       { return !warnings.isEmpty(); }
    public boolean hasErrors()         { return !errors.isEmpty(); }

    public void setImportedCount(int c) { importedCount = c; }
    public void setStatus(Status s)     { status = s; }
}