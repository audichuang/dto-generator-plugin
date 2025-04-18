package com.catchaybk.dtogeneratorplugin.core.model;

import com.catchaybk.dtogeneratorplugin.core.config.FieldConfig;
import com.catchaybk.dtogeneratorplugin.intellij.ui.dialog.ValidationMessageSettingDialog;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 字段模型類
 * 表示DTO中的一個字段，包含所有字段相關的屬性和行為
 * <p>
 * 主要功能：
 * 1. 管理字段的基本信息（名稱、類型、大小等）
 * 2. 處理字段類型的格式化和驗證
 * 3. 提供字段相關的工具方法
 * 4. 管理字段的導入聲明和驗證註解
 */
@Getter
@Setter
public class Field {
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
        return FieldConfig.PRIMITIVE_AND_WRAPPER_TYPES.contains(type.toLowerCase());
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
        return FieldConfig.TYPE_FORMAT_MAP.getOrDefault(lowercaseType, typeName);
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
            String importPath = FieldConfig.TYPE_IMPORT_MAP.get(
                    lowerType.startsWith("list") ? "List"
                            : Character.toUpperCase(lowerType.charAt(0)) + lowerType.substring(1));

            if (importPath != null) {
                imports.add(importPath);
            }
            if (lowerType.contains("date")) {
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
                String smallCamel = toCamelCase(dataName, false);
                // 確保第一個字母小寫
                return Character.toLowerCase(smallCamel.charAt(0)) + smallCamel.substring(1);
            case "大駝峰":
                String bigCamel = toCamelCase(dataName, true);
                // 確保第一個字母大寫
                return Character.toUpperCase(bigCamel.charAt(0)) + bigCamel.substring(1);
            case "無":
                return null; // 不添加 JsonAlias
            default:
                return dataName;
        }
    }

    private String toUpperSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 先處理駝峰命名的情況
        StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(input.charAt(0)));

        for (int i = 1; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (Character.isUpperCase(currentChar)) {
                result.append('_').append(currentChar);
            } else {
                result.append(Character.toUpperCase(currentChar));
            }
        }

        return result.toString();
    }

    /**
     * 將字符串轉換為駝峰命名格式
     * 處理以下情況：
     * 1. 已經是駝峰：ProposalEntityList -> proposalEntityList/ProposalEntityList
     * 2. 底線分隔：proposal_entity_list -> proposalEntityList/ProposalEntityList
     * 3. 空格分隔：proposal entity list -> proposalEntityList/ProposalEntityList
     *
     * @param input           輸入字符串
     * @param capitalizeFirst 是否首字母大寫
     * @return 駝峰命名格式的字符串
     */
    private String toCamelCase(String input, boolean capitalizeFirst) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 1. 先處理底線和空格分隔的情況
        if (input.contains("_") || input.contains(" ")) {
            String[] parts = input.split("[_\\s]+");
            StringBuilder camelCase = new StringBuilder();

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].toLowerCase();
                if (i == 0 && !capitalizeFirst) {
                    camelCase.append(part);
                } else {
                    camelCase.append(Character.toUpperCase(part.charAt(0)))
                            .append(part.substring(1).toLowerCase());
                }
            }
            return camelCase.toString();
        }

        // 2. 處理已經是駝峰命名的情況
        StringBuilder result = new StringBuilder();
        StringBuilder currentWord = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            // 檢測單詞邊界
            if (i > 0 && Character.isUpperCase(ch)) {
                // 完成前一個單詞
                if (currentWord.length() > 0) {
                    appendWord(result, currentWord.toString(), result.length() == 0 && !capitalizeFirst);
                    currentWord = new StringBuilder();
                }
            }
            currentWord.append(ch);
        }

        // 處理最後一個單詞
        if (currentWord.length() > 0) {
            appendWord(result, currentWord.toString(), result.length() == 0 && !capitalizeFirst);
        }

        return result.toString();
    }

    /**
     * 添加單詞到結果中
     */
    private void appendWord(StringBuilder result, String word, boolean toLowerCase) {
        if (word.length() == 0)
            return;

        if (toLowerCase) {
            result.append(Character.toLowerCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase());
        } else {
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase());
        }
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

        // 添加 Valid 註解（用於包含需要驗證對象的List）
        if (isList() && dataType != null) {
            String genericType = "";
            if (dataType.contains("<") && dataType.contains(">")) {
                genericType = dataType.substring(dataType.indexOf("<") + 1, dataType.lastIndexOf(">")).trim();
            }
            if (!isPrimitiveOrWrapperType(genericType)) {
                annotations.add("@Valid");
            }
        }

        return String.join("\n    ", annotations);
    }

}