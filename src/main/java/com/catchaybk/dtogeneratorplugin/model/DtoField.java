package com.catchaybk.dtogeneratorplugin.model;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class DtoField {
    private int level;
    private String dataName;
    private String dataType;
    private String size;
    private boolean nullable;
    private String comments;
    private String childClassName;
    private boolean isObject;
    private static final Map<String, String> TYPE_IMPORT_MAP = new HashMap<>();

    static {
        // 初始化類型的導入路徑映射
        TYPE_IMPORT_MAP.put("BigDecimal", "java.math.BigDecimal");
        TYPE_IMPORT_MAP.put("LocalDate", "java.time.LocalDate");
        TYPE_IMPORT_MAP.put("LocalDateTime", "java.time.LocalDateTime");
        TYPE_IMPORT_MAP.put("Date", "java.util.Date");
        TYPE_IMPORT_MAP.put("List", "java.util.List");
    }


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

        // 處理泛型類型，例如 List<String>
        if (type.toLowerCase().trim().startsWith("list<")) {
            // 提取泛型參數，但保持原始大小寫
            String genericType = type.substring(type.indexOf('<') + 1, type.lastIndexOf('>')).trim();
            return isPrimitiveOrWrapperType(genericType);
        }

        return isPrimitiveOrWrapperType(type);
    }


    public boolean isPrimitiveOrWrapperType(String type) {
        // 創建一個不區分大小寫的比較集合
        Set<String> primitiveAndWrapperTypes = new HashSet<>(Arrays.asList(
                "string", "String",
                "int", "integer", "Integer",
                "long", "Long",
                "double", "Double",
                "float", "Float",
                "boolean", "Boolean",
                "date", "Date",
                "datetime", "DateTime",
                "bigdecimal", "BigDecimal",
                "char", "Character",
                "byte", "Byte",
                "short", "Short",
                "void", "Void",
                "decimal", "Decimal",
                "number", "Number",
                "LocalDate",
                "LocalDateTime"
        ));

        // 使用原始類型進行比較，而不是轉換為小寫
        return primitiveAndWrapperTypes.contains(type);
    }


    public boolean isList() {
        if (dataType == null) return false;
        String type = dataType.trim();
        return type.equalsIgnoreCase("list") || // 添加對純 List 的支援
                type.toLowerCase().startsWith("list<");
    }


    public boolean isObject() {
        if (dataType == null) return false;

        String type = dataType.trim();

        // 處理純 List 類型（沒有泛型參數）
        if (type.equalsIgnoreCase("list")) {
            return true; // 沒有指定泛型的 List 應該被視為需要創建新類
        }

        // 處理泛型類型
        if (type.startsWith("List<") || type.startsWith("list<")) {
            String genericType = type.substring(type.indexOf('<') + 1, type.lastIndexOf('>')).trim();
            return !isPrimitiveOrWrapperType(genericType);
        }

        // 處理其他類型
        return type.equals("Object") || (!isPrimitiveType(type) && !isList());
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

    public String getFormattedDataType() {
        if (dataType == null) return "";

        // 如果是 List 類型，需要特別處理
        if (dataType.trim().toLowerCase().startsWith("list<")) {
            int start = dataType.indexOf('<');
            int end = dataType.lastIndexOf('>');
            if (start >= 0 && end >= 0) {
                String genericType = dataType.substring(start + 1, end).trim();
                // 確保泛型參數使用正確的大小寫
                String formattedGenericType = formatTypeName(genericType);
                return "List<" + formattedGenericType + ">";
            }
        }

        // 非 List 類型，直接返回格式化後的類型名
        return formatTypeName(dataType.trim());
    }


    public String getCapitalizedName() {
        if (dataName == null || dataName.isEmpty()) {
            return "";
        }
        return dataName.substring(0, 1).toUpperCase() + dataName.substring(1);
    }

    private String formatTypeName(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return typeName;
        }

        // 將輸入轉換為小寫以進行比較
        String lowercaseType = typeName.toLowerCase().trim();

        // 基本類型的標準化映射
        Map<String, String> typeMapping = new HashMap<>();
        typeMapping.put("string", "String");
        typeMapping.put("integer", "Integer");
        typeMapping.put("int", "Integer");
        typeMapping.put("long", "Long");
        typeMapping.put("double", "Double");
        typeMapping.put("float", "Float");
        typeMapping.put("boolean", "Boolean");
        typeMapping.put("date", "Date");
        typeMapping.put("datetime", "LocalDateTime");
        typeMapping.put("bigdecimal", "BigDecimal");
        typeMapping.put("decimal", "BigDecimal");
        typeMapping.put("char", "Character");
        typeMapping.put("byte", "Byte");
        typeMapping.put("short", "Short");
        typeMapping.put("void", "Void");
        typeMapping.put("number", "BigDecimal");
        typeMapping.put("localdate", "LocalDate");
        typeMapping.put("localdatetime", "LocalDateTime");

        // 返回標準格式的類型名
        return typeMapping.getOrDefault(lowercaseType, typeName);
    }

    public Set<String> getRequiredImports() {
        Set<String> imports = new HashSet<>();
        String formattedType = getFormattedDataType();

        // 處理泛型類型
        if (formattedType.toLowerCase().startsWith("list<")) {
            imports.add("java.util.List");

            // 提取泛型參數
            String genericType = formattedType.substring(5, formattedType.length() - 1).trim();
            String genericImport = TYPE_IMPORT_MAP.get(formatTypeName(genericType));
            if (genericImport != null) {
                imports.add(genericImport);
            }
        } else {
            // 處理普通類型
            String typeImport = TYPE_IMPORT_MAP.get(formatTypeName(formattedType));
            if (typeImport != null) {
                imports.add(typeImport);
            }
        }

        return imports;
    }


}