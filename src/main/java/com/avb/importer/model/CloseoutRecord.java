package com.avb.importer.model;

public class CloseoutRecord {
    private String avbSku;
    private String closeoutId;
    private int linqId;
    private String avbStatus;
    private String closeoutType;
    private String avbBrand;

    public CloseoutRecord() {}

    public CloseoutRecord(String avbSku, String closeoutId, int linqId,
                          String avbStatus, String closeoutType, String avbBrand) {
        this.avbSku = avbSku;
        this.closeoutId = closeoutId;
        this.linqId = linqId;
        this.avbStatus = avbStatus;
        this.closeoutType = closeoutType;
        this.avbBrand = avbBrand;
    }

    public String getAvbSku()         { return avbSku; }
    public String getCloseoutId()     { return closeoutId; }
    public int getLinqId()            { return linqId; }
    public String getAvbStatus()      { return avbStatus; }
    public String getCloseoutType()   { return closeoutType; }
    public String getAvbBrand()       { return avbBrand; }

    public void setAvbSku(String v)       { avbSku = v; }
    public void setCloseoutId(String v)   { closeoutId = v; }
    public void setLinqId(int v)          { linqId = v; }
    public void setAvbStatus(String v)    { avbStatus = v; }
    public void setCloseoutType(String v) { closeoutType = v; }
    public void setAvbBrand(String v)     { avbBrand = v; }
}