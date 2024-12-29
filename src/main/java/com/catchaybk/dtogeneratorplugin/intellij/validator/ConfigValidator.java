package com.catchaybk.dtogeneratorplugin.intellij.validator;

import com.catchaybk.dtogeneratorplugin.core.model.ConfigData;
import com.catchaybk.dtogeneratorplugin.core.validator.DtoValidator;
import com.intellij.openapi.ui.ValidationInfo;

import javax.swing.*;

/**
 * 配置驗證器
 * 負責驗證 IntelliJ IDEA 環境下的 DTO 配置有效性
 * <p>
 * 主要驗證：
 * 1. 主類名的有效性
 * 2. 目標包路徑的有效性
 * 3. 其他必要配置的完整性
 */
public class ConfigValidator {
    /**
     * 驗證 DTO 配置的有效性
     *
     * @param config         要驗證的配置數據
     * @param mainClassField 主類名輸入框組件（用於在驗證失敗時高亮顯示）
     * @return 驗證結果，null 表示驗證通過，否則返回錯誤信息
     */
    public static ValidationInfo validate(ConfigData config, JComponent mainClassField) {
        // 使用核心驗證邏輯進行基礎驗證
        if (!DtoValidator.validateConfig(config.getMainClassName(), config.getTargetPackage())) {
            return new ValidationInfo("主類名和目標包路徑不能為空", mainClassField);
        }
        return null;
    }
}