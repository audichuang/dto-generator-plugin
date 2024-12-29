package com.catchaybk.dtogeneratorplugin.model;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class DtoField {
    private static final Map<String, String> TYPE_IMPORT_MAP = new HashMap<>();

    static {
        TYPE_IMPORT_MAP.put("Timestamp", "java.sql.Timestamp");
        TYPE_IMPORT_MAP.put("BigDecimal", "java.math.BigDecimal");
        TYPE_IMPORT_MAP.put("LocalDate", "java.time.LocalDate");
        TYPE_IMPORT_MAP.put("LocalDateTime", "java.time.LocalDateTime");
        TYPE_IMPORT_MAP.put("Date", "java.util.Date");
        TYPE_IMPORT_MAP.put("List", "java.util.List");
    }

    private final boolean isJava17;
    private int level;
    private String dataName;
    private String dataType;
    private String size;
    private boolean required;
    private String comments;
    private String childClassName;
    private boolean isObject;
    private String requiredString;

    public DtoField(int level, String dataName, String dataType, String size, boolean required, String comments,
                    boolean isJava17) {
        this.level = level;
        this.dataName = dataName;
        this.dataType = dataType;
        this.size = size;
        this.required = required;
        this.comments = comments;
        this.isJava17 = isJava17;
        this.isObject = !isPrimitiveType(dataType);
    }

    private boolean isPrimitiveType(String type) {
        if (type == null)
            return false;

        // 處理泛型類型，例如 List<String>
        if (type.toLowerCase().trim().startsWith("list<")) {
            // 提取泛型參數，但保持原始大小寫
            String genericType = type.substring(type.indexOf('<') + 1, type.lastIndexOf('>')).trim();
            return isPrimitiveOrWrapperType(genericType);
        }

        return isPrimitiveOrWrapperType(type);
    }

    public boolean isPrimitiveOrWrapperType(String type) {
        Set<String> primitiveAndWrapperTypes = new HashSet<>(Arrays.asList(
                "string", "String",
                "int", "integer", "Integer",
                "long", "Long",
                "double", "Double",
                "float", "Float",
                "boolean", "Boolean",
                "date", "Date",
                "datetime", "DateTime",
                "timestamp", "Timestamp",
                "bigdecimal", "BigDecimal",
                "decimal", "BigDecimal",
                "char", "Character",
                "byte", "Byte",
                "short", "Short",
                "void", "Void",
                "LocalDate",
                "LocalDateTime"));

        return primitiveAndWrapperTypes.contains(type.toLowerCase());
    }

    public boolean isList() {
        if (dataType == null)
            return false;
        String type = dataType.trim();
        return type.equalsIgnoreCase("list") || // 添加對純 List 的支援
                type.toLowerCase().startsWith("list<");
    }

    public boolean isObject() {
        if (dataType == null)
            return false;

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
        if (childClassName != null) {
            return isList() ? "List<" + childClassName + ">" : childClassName;
        }
        return formatTypeName(dataType);
    }

    private String formatTypeName(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return typeName;
        }

        String lowercaseType = typeName.toLowerCase().trim();
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
        typeMapping.put("timestamp", "Timestamp");
        typeMapping.put("bigdecimal", "BigDecimal");
        typeMapping.put("decimal", "BigDecimal");
        typeMapping.put("char", "Character");
        typeMapping.put("byte", "Byte");
        typeMapping.put("short", "Short");
        typeMapping.put("void", "Void");
        typeMapping.put("localdate", "LocalDate");
        typeMapping.put("localdatetime", "LocalDateTime");

        return typeMapping.getOrDefault(lowercaseType, typeName);
    }

    public String getCapitalizedName() {
        if (dataName == null || dataName.isEmpty()) {
            return "";
        }
        return dataName.substring(0, 1).toUpperCase() + dataName.substring(1);
    }

    public Set<String> getRequiredImports() {
        Set<String> imports = new HashSet<>();
        if (dataType != null) {
            String lowerType = dataType.toLowerCase();
            if (lowerType.startsWith("list")) {
                imports.add("java.util.List");
            } else if (lowerType.contains("date")) {
                imports.add("java.util.Date");
            } else if (lowerType.contains("timestamp")) {
                imports.add("java.sql.Timestamp");
            } else if (lowerType.equals("bigdecimal") || lowerType.equals("decimal")) {
                imports.add("java.math.BigDecimal");
            }

            // 果有 size 格式，添加 Digits 註解
            if ((lowerType.equals("decimal") || lowerType.equals("bigdecimal"))
                    && !size.isEmpty()) {
                // 根據 Java 版本選擇正確的包
                String validationPackage = isJava17 ? "jakarta.validation.constraints" : "javax.validation.constraints";
                imports.add(validationPackage + ".Digits");
            }
        }
        return imports;
    }

    public void setRequiredString(String requiredString) {
        this.requiredString = requiredString;
        this.required = "Y".equalsIgnoreCase(requiredString); // 只有當值為 "Y" 時才設為 true
    }

    public String formatName(String style) {
        if (style == null || style.equals("原始格式")) {
            return dataName;
        }

        switch (style) {
            case "全大寫":
                return dataName.toUpperCase();
            case "全小寫":
                return dataName.toLowerCase();
            case "大寫底線":
                return toUpperSnakeCase(dataName);
            case "小駝峰":
                return toCamelCase(dataName, false);
            case "大駝峰":
                return toCamelCase(dataName, true);
            case "無":
                return null; // 不添加 JsonAlias
            default:
                return dataName;
        }
    }

    private String toUpperSnakeCase(String input) {
        String regex = "([a-z])([A-Z])";
        String replacement = "$1_$2";
        return input.replaceAll(regex, replacement).toUpperCase();
    }

    private String toCamelCase(String input, boolean capitalizeFirst) {
        String[] parts = input.split("[_\\s]+");
        StringBuilder camelCase = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            if (i == 0 && !capitalizeFirst) {
                camelCase.append(part);
            } else {
                camelCase.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1));
            }
        }

        return camelCase.toString();
    }

}