package com.catchaybk.dtogeneratorplugin.core.generator;

/**
 * 名稱生成器
 * 負責根據不同條件生成DTO類的名稱
 * <p>
 * 命名規則：
 * 1. 無電文ID時：
 * - 主類：使用指定名稱或默認為 "MainDTO"
 * - 子類：首字母大寫的字段名
 * 2. 有電文ID時：
 * - 主類：[電文ID] + [方向後綴(Tranrq/Tranrs)]
 * - 子類：[電文ID] + [方向後綴] + [首字母大寫的字段名]
 */
public class ClassNameGenerator {
    /**
     * 生成DTO類名
     *
     * @param msgId       消息ID，用於生成類名前綴
     * @param direction   電文方向（上行/下行/無），決定類名後綴
     * @param baseName    基礎名稱，用於子類命名
     * @param isMainClass 是否為主類
     * @return 生成的類名
     */
    public static String generateClassName(String msgId, String direction,
                                           String baseName, boolean isMainClass) {
        if (msgId == null || msgId.isEmpty() || "無".equals(direction)) {
            return isMainClass ? (baseName.isEmpty() ? "MainDTO" : baseName)
                    : capitalizeFirstLetter(baseName);
        }

        String suffix = "上行".equals(direction) ? "Tranrq" : "Tranrs";
        String prefix = msgId.toUpperCase();
        return isMainClass ? prefix + suffix : prefix + suffix + capitalizeFirstLetter(baseName);
    }

    /**
     * 將字符串的首字母轉為大寫
     *
     * @param input 輸入字符串
     * @return 首字母大寫的字符串
     */
    private static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}