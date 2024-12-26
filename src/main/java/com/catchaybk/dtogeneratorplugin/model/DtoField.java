package com.catchaybk.dtogeneratorplugin.model;

public class DtoField {
    private int level;
    private String dataName;
    private String dataType;
    private String size;
    private boolean nullable;
    private String comments;

    public DtoField(int level, String dataName, String dataType, String size, boolean nullable, String comments) {
        this.level = level;
        this.dataName = dataName;
        this.dataType = dataType;
        this.size = size;
        this.nullable = nullable;
        this.comments = comments;
    }

    // Getters
    public int getLevel() {
        return level;
    }

    public String getDataName() {
        return dataName;
    }

    public String getDataType() {
        return dataType;
    }

    public String getSize() {
        return size;
    }

    public boolean isNullable() {
        return nullable;
    }

    public String getComments() {
        return comments;
    }

    // Setters
    public void setLevel(int level) {
        this.level = level;
    }

    public void setDataName(String dataName) {
        this.dataName = dataName;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    // 新增方法：獲取駝峰命名的變數名
    public String getCamelCaseName() {
        if (dataName == null || dataName.isEmpty()) {
            return "";
        }
        return dataName.substring(0, 1).toLowerCase() + dataName.substring(1);
    }

    // 新增方法：獲取原始名稱（用於JsonProperty）
    public String getOriginalName() {
        return dataName;
    }
}