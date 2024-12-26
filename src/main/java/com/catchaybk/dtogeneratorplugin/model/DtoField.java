package com.catchaybk.dtogeneratorplugin.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DtoField {
    private int level;
    private String dataName;
    private String dataType;
    private String size;
    private boolean nullable;
    private String comments;
    private String childClassName;
    private boolean isObject; // 新增：標記是否為自定義對象類型

    public DtoField(int level, String dataName, String dataType, String size, boolean nullable, String comments) {
        this.level = level;
        this.dataName = dataName;
        this.dataType = dataType;
        this.size = size;
        this.nullable = nullable;
        this.comments = comments;
        this.isObject = !isPrimitiveType(dataType);
    }

    private boolean isPrimitiveType(String type) {
        if (type == null) return false;
        String lowerType = type.toLowerCase().trim();

        // 明確檢查是否為對象或列表類型
        if (lowerType.equals("object") ||
                lowerType.startsWith("list") ||
                lowerType.contains("list<")) {
            return false;
        }

        Set<String> primitiveTypes = new HashSet<>(Arrays.asList(
                "string", "int", "integer", "long", "double", "float",
                "boolean", "date", "datetime", "bigdecimal", "char",
                "byte", "short", "void", "decimal"
        ));

        return primitiveTypes.contains(lowerType);
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

    public String getChildClassName() {
        return childClassName;
    }

    public void setChildClassName(String childClassName) {
        this.childClassName = childClassName;
    }
    public boolean isList() {
        if (dataType == null) return false;
        String lowerType = dataType.toLowerCase().trim();
        return lowerType.equals("list<object>") ||
                lowerType.equals("list") ||
                lowerType.contains("list<") ||
                lowerType.startsWith("list");
    }

    public boolean isObject() {
        if (dataType == null) return false;
        String lowerType = dataType.toLowerCase().trim();
        return lowerType.equals("object") ||
                (!isPrimitiveType(lowerType) && !isList());
    }

    public String getCamelCaseName() {
        if (dataName == null || dataName.isEmpty()) {
            return "";
        }
        String firstChar = dataName.substring(0, 1).toLowerCase();
        return firstChar + (dataName.length() > 1 ? dataName.substring(1) : "");
    }

    public String getOriginalName() {
        return dataName;
    }
}