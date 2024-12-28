package com.catchaybk.dtogeneratorplugin.generator;

/**
 * DTO名稱生成器
 * 負責生成各種DTO相關的類名
 */
public class DtoNameGenerator {
    /**
     * 生成DTO類名
     *
     * @param msgId       消息ID
     * @param direction   電文方向
     * @param baseName    基礎名稱
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

    private static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}