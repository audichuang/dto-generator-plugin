package com.catchaybk.dtogeneratorplugin.intellij.validator;

import com.catchaybk.dtogeneratorplugin.core.model.DtoConfigData;
import com.catchaybk.dtogeneratorplugin.core.validator.DtoValidator;
import com.intellij.openapi.ui.ValidationInfo;
import javax.swing.JComponent;

public class DtoConfigValidator {
    public static ValidationInfo validate(DtoConfigData config, JComponent mainClassField) {
        if (!DtoValidator.validateConfig(config.getMainClassName(), config.getTargetPackage())) {
            return new ValidationInfo("主類名和目標包路徑不能為空", mainClassField);
        }
        return null;
    }
}