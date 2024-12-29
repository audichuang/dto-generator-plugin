package com.catchaybk.dtogeneratorplugin.core.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

/**
 * 字段配置類
 * 集中管理字段相關的配置信息
 */
public class FieldConfig {
    /** 類型導入映射表，用於管理需要特殊導入的類型 */
    public static final Map<String, String> TYPE_IMPORT_MAP = new HashMap<>() {
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
    public static final Set<String> PRIMITIVE_AND_WRAPPER_TYPES = new HashSet<>(Arrays.asList(
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

    /** 類型格式化映射表 */
    public static final Map<String, String> TYPE_FORMAT_MAP = new HashMap<>() {
        {
            put("string", "String");
            put("integer", "Integer");
            put("int", "Integer");
            put("long", "Long");
            put("double", "Double");
            put("float", "Float");
            put("boolean", "Boolean");
            put("date", "Date");
            put("datetime", "LocalDateTime");
            put("timestamp", "Timestamp");
            put("bigdecimal", "BigDecimal");
            put("decimal", "BigDecimal");
            put("char", "Character");
            put("byte", "Byte");
            put("short", "Short");
            put("void", "Void");
            put("localdate", "LocalDate");
            put("localdatetime", "LocalDateTime");
        }
    };
}