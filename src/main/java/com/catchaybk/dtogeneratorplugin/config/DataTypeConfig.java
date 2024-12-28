package com.catchaybk.dtogeneratorplugin.config;

import java.util.HashSet;
import java.util.Set;

public class DataTypeConfig {
    private static final Set<String> KNOWN_TYPES = new HashSet<>();

    static {
        // 基本類型
        addTypes(
                "string",
                "int", "integer",
                "long",
                "double",
                "float",
                "boolean",
                "char",
                "byte",
                "short",
                "void");

        // 日期時間類型
        addTypes(
                "date",
                "datetime",
                "timestamp",
                "localdate",
                "localdatetime");

        // 其他常用類型
        addTypes(
                "bigdecimal",
                "decimal",
                "number",
                "list",
                "object");
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
                || lowerType.endsWith("dto")
                || type.contains(".");
    }
}