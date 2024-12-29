package com.catchaybk.dtogeneratorplugin.core.config;

import java.util.HashSet;
import java.util.Set;

/**
 * 類型註冊表
 * 管理和驗證所有支持的數據類型
 */
public class TypeRegistry {
    private static final Set<String> KNOWN_TYPES = new HashSet<>();

    static {
        // 基本類型
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

        // 數字類型
        addTypes(
                "decimal", // 用於表示精確的十進制數
                "bigdecimal"); // Java BigDecimal 類型

        // 日期時間類型
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

    private static void addTypes(String... types) {
        for (String type : types) {
            KNOWN_TYPES.add(type.toLowerCase());
        }
    }

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