package com.catchaybk.dtogeneratorplugin.core.config;

import java.util.HashSet;
import java.util.Set;

/**
 * 數據類型配置管理器
 * 負責管理和驗證所有支持的數據類型
 * 
 * 支持的類型包括：
 * 1. 基本類型（int, long, String 等）
 * 2. 包裝類型（Integer, Long 等）
 * 3. 日期時間類型（Date, LocalDateTime 等）
 * 4. 數字類型（BigDecimal 等）
 * 5. 集合類型（List）
 */
public class DataTypeConfig {
    /** 已知類型集合，存儲所有支持的類型（小寫形式） */
    private static final Set<String> KNOWN_TYPES = new HashSet<>();

    static {
        // 初始化基本類型
        addTypes(
                "string", // 字符串類型
                "int", "integer", // 整數類型
                "long", // 長整數類型
                "double", // 雙精度浮點數
                "float", // 單精度浮點數
                "boolean", // 布爾類型
                "char", // 字符類型
                "byte", // 字節類型
                "short", // 短整數類型
                "void"); // 空類型

        // 初始化數字類型
        addTypes(
                "decimal", // 用於表示精確的十進制數
                "bigdecimal"); // Java BigDecimal 類型

        // 初始化日期時間類型
        addTypes(
                "date", // 日期類型
                "datetime", // 日期時間類型
                "timestamp", // 時間戳類型
                "localdate", // Java 8+ 本地日期
                "localdatetime"); // Java 8+ 本地日期時間

        // 其他常用類型
        addTypes(
                "list", // 列表類型
                "object"); // 對象類型
    }

    /**
     * 添加多個類型到已知類型集合中
     * 所有類型都會被轉換為小寫存儲
     *
     * @param types 要添加的類型數組
     */
    private static void addTypes(String... types) {
        for (String type : types) {
            KNOWN_TYPES.add(type.toLowerCase());
        }
    }

    /**
     * 檢查給定的類型是否為已知類型
     * 支持檢查：
     * 1. 簡單類型（如 String, Integer 等）
     * 2. 泛型類型（如 List<String>）
     * 3. DTO 類型（以 dto 結尾）
     * 4. 帶包名的完整類型（包含 . 的類型）
     *
     * @param type 要檢查的類型名稱
     * @return 如果是已知類型則返回 true
     */
    public static boolean isKnownType(String type) {
        if (type == null)
            return false;

        // 處理泛型類型，例如 List<String>
        if (type.contains("<")) {
            String baseType = type.substring(0, type.indexOf("<"));
            String genericType = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
            return isKnownType(baseType.trim()) && isKnownType(genericType.trim());
        }

        String lowerType = type.toLowerCase();
        return KNOWN_TYPES.contains(lowerType)
                || lowerType.endsWith("dto") // DTO 類型
                || type.contains("."); // 帶包名的完整類型
    }
}