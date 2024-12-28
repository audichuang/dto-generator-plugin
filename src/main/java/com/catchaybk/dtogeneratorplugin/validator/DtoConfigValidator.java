package com.catchaybk.dtogeneratorplugin.validator;

import com.catchaybk.dtogeneratorplugin.model.DtoConfigData;
import com.intellij.openapi.ui.ValidationInfo;
import javax.swing.*;

/**
 * DTO配置驗證器
 * 負責驗證DTO配置的有效性
 */
public class DtoConfigValidator {
    /**
     * 驗證配置是否有效
     * 
     * @param config         配置數據
     * @param mainClassField 主類名輸入框組件
     * @return 驗證結果，null表示驗證通過
     */
    public static ValidationInfo validate(DtoConfigData config, JComponent mainClassField) {
        // 驗證主類名
        if (config.getClassNames().get("main") == null ||
                config.getClassNames().get("main").trim().isEmpty()) {
            return new ValidationInfo("主類名不能為空", mainClassField);
        }

        // 驗證包名
        if (config.getTargetPackage() == null || config.getTargetPackage().trim().isEmpty()) {
            return new ValidationInfo("目標包路徑不能為空");
        }

        return null;
    }
}