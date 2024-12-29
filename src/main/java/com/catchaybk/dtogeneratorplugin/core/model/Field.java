package com.catchaybk.dtogeneratorplugin.core.model;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

import com.catchaybk.dtogeneratorplugin.intellij.ui.dialog.ValidationMessageSettingDialog;

/**
 * DTO字段模型類
 * 表示DTO中的一個字段，包含所有字段相關的屬性和行為
 * 
 * 主要功能：
 * 1. 管理字段的基本信息（名稱、類型、大小等）
 * 2. 處理字段類型的格式化和驗證
 * 3. 提供字段相關的工具方法
 * 4. 管理字段的導入聲明和驗證註解
 */
@Getter
@Setter
public class Field {
    /** 類型導入映射表，用於管理需要特殊導入的類型 */
    private static final Map<String, String> TYPE_IMPORT_MAP = new HashMap<>() {
        {
            put("Timestamp", "java.sql.Timestamp");
            put("BigDecimal", "java.math.BigDecimal");
            put("LocalDate", "java.time.LocalDate");
            put("LocalDateTime", "java.time.LocalDateTime");
            put("Date", "java.util.Date");
            put("List", "java.util.List");
        }
    };

    /** 基本類型和包裝類型集合 */
    private static final Set<String> PRIMITIVE_AND_WRAPPER_TYPES = new HashSet<>(Arrays.asList(
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

    // 字段基本屬性
    private final boolean isJava17; // 是否使用 Java 17
    private int level; // 字段層級
    private String dataName; // 字段名稱
    private String dataType; // 數據類型
    private String size; // 大小限制
    private boolean required; // 是否必填
    private String comments; // 註解說明
    private String pattern; // 正則表達式模式
    private String childClassName; // 子類名稱（用於複雜類型）
    private boolean isObject; // 是否為對象類型
    private String requiredString; // 必填標記字符串

    /**
     * 創建字段實例
     *
     * @param level    字段層級
     * @param dataName 字段名稱
     * @param dataType 數據類型
     * @param size     大小限制
     * @param required 是否必填
     * @param comments 註解說明
     * @param pattern  正則表達式模式
     * @param isJava17 是否使用Java 17
     */
    public Field(int level, String dataName, String dataType, String size,
            boolean required, String comments, String pattern, boolean isJava17) {
        this.level = level;
        this.dataName = dataName;
        this.dataType = dataType;
        this.size = size;
        this.required = required;
        this.comments = comments;
        this.pattern = pattern;
        this.isJava17 = isJava17;
        this.isObject = !isPrimitiveType(dataType);
    }

    /**
     * 判斷給定類型是否為原始類型
     * 支持處理泛型類型，如 List<String>
     *
     * @param type 要判斷的類型
     * @return 如果是原始類型返回true
     */
    private boolean isPrimitiveType(String type) {
        if (type == null)
            return false;

        // 處理泛型類型，例如 List<String>
        if (type.toLowerCase().trim().startsWith("list<")) {
            String genericType = type.substring(type.indexOf('<') + 1, type.lastIndexOf('>')).trim();
            return isPrimitiveOrWrapperType(genericType);
        }

        return isPrimitiveOrWrapperType(type);
    }

    /**
     * 判斷類型是否為原始類型或其包裝類型
     * 包括：
     * 1. 基本類型（int, long等）
     * 2. 包裝類型（Integer, Long等）
     * 3. 常用類型（String, Date等）
     *
     * @param type 要判斷的類型
     * @return 如果是原始類型或包裝類型返回true
     */
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

    /**
     * 判斷字段是否為List類型
     * 支持兩種形式：
     * 1. 純List類型
     * 2. 帶泛型的List類型（如List<String>）
     *
     * @return 如果是List類型返回true
     */
    public boolean isList() {
        if (dataType == null)
            return false;
        String type = dataType.trim();
        return type.equalsIgnoreCase("list") ||
                type.toLowerCase().startsWith("list<");
    }

    /**
     * 判斷字段是否為對象類型
     * 包括：
     * 1. 純對象類型
     * 2. 純List類型（無泛型參數）
     * 3. 包含非原始類型的List
     *
     * @return 如果是對象類型返回true
     */
    public boolean isObject() {
        if (dataType == null)
            return false;

        String type = dataType.trim();

        if (type.equalsIgnoreCase("list")) {
            return true; // 純List類型視為對象
        }

        if (type.startsWith("List<") || type.startsWith("list<")) {
            String genericType = type.substring(type.indexOf('<') + 1, type.lastIndexOf('>')).trim();
            return !isPrimitiveOrWrapperType(genericType);
        }

        return type.equals("Object") || (!isPrimitiveType(type) && !isList());
    }

    /**
     * 獲取字段的駝峰命名形式
     * 例如：USER_NAME -> userName
     *
     * @return 駝峰命名的字段名
     */
    public String getCamelCaseName() {
        if (dataName == null || dataName.isEmpty()) {
            return "";
        }
        String firstChar = dataName.substring(0, 1).toLowerCase();
        return firstChar + (dataName.length() > 1 ? dataName.substring(1) : "");
    }

    /**
     * 獲取原始字段名
     *
     * @return 原始字段名
     */
    public String getOriginalName() {
        return dataName;
    }

    /**
     * 獲取格式化後的數據類型
     * 處理：
     * 1. 子類類型
     * 2. List類型的泛型
     * 3. 基本類型的標準化
     *
     * @return 格式化後的類型名稱
     */
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
        String validationPackage = isJava17 ? "jakarta.validation.constraints" : "javax.validation.constraints";

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

            // 添加 Pattern 相關的導入
            if (pattern != null && !pattern.isEmpty()) {
                imports.add(validationPackage + ".Pattern");
            }

            // 添加其他驗證註解的導入
            if (required) {
                imports.add(validationPackage + ".NotNull");
            }

            // 添加 Digits 註解的導入
            if ((lowerType.equals("decimal") || lowerType.equals("bigdecimal")) && !size.isEmpty()) {
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

    /**
     * 獲取字段的驗證註解
     * 包括：
     * 1. Pattern 註解（如果有正則表達式）
     * 2. NotNull/NotBlank 註解（如果是必填字段）
     * 3. Size 註解（如果是字符串且有長度限制）
     * 4. Digits 註解（如果是數字類型且有大小限制）
     *
     * @return 驗證註解字符串
     */
    public String getValidationAnnotations() {
        List<String> annotations = new ArrayList<>();

        // 添加 Pattern 註解
        if (pattern != null && !pattern.isEmpty()) {
            annotations.add(String.format("@Pattern(regexp = \"%s\", message = \"%s\")",
                    pattern,
                    ValidationMessageSettingDialog.getPatternMessage(getCamelCaseName(), comments)));
        }

        // 添加 NotNull/NotBlank 註解
        if (required) {
            if (dataType != null && dataType.toLowerCase().contains("string")) {
                annotations.add(String.format("@NotBlank(message = \"%s\")",
                        ValidationMessageSettingDialog.getNotBlankMessage(getCamelCaseName(), comments)));
            } else {
                annotations.add(String.format("@NotNull(message = \"%s\")",
                        ValidationMessageSettingDialog.getNotNullMessage(getCamelCaseName(), comments)));
            }
        }

        // 添加 Size 註解（用於字符串類型）
        if (dataType != null && dataType.toLowerCase().contains("string") && !size.isEmpty()) {
            annotations.add(String.format("@Size(max = %s, message = \"%s\")",
                    size,
                    ValidationMessageSettingDialog.getSizeMessage(getCamelCaseName(), comments, size)));
        }

        // 添加 Digits 註解（用於數字類型）
        if (dataType != null && (dataType.equalsIgnoreCase("decimal") ||
                dataType.equalsIgnoreCase("bigdecimal")) && !size.isEmpty()) {
            String[] parts = size.split(",");
            String integer = parts[0];
            String fraction = parts.length > 1 ? parts[1] : "0";
            annotations.add(String.format("@Digits(integer = %s, fraction = %s, message = \"%s\")",
                    integer, fraction,
                    ValidationMessageSettingDialog.getDigitsMessage(getCamelCaseName(), comments, size)));
        }

        return String.join("\n    ", annotations);
    }

}