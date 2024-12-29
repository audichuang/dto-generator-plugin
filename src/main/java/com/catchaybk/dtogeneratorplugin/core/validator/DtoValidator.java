package com.catchaybk.dtogeneratorplugin.core.validator;

public class DtoValidator {
    // 純邏輯驗證，不依賴 IDE API
    public static boolean validateConfig(String mainClassName, String targetPackage) {
        return mainClassName != null && !mainClassName.trim().isEmpty()
                && targetPackage != null && !targetPackage.trim().isEmpty();
    }
}