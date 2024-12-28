package com.catchaybk.dtogeneratorplugin.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ValidationMessageSettingDialog extends DialogWrapper {
    private static final String NOTBLANK_KEY = "dto.generator.validation.notblank";
    private static final String NOTNULL_KEY = "dto.generator.validation.notnull";
    private static final String SIZE_KEY = "dto.generator.validation.size";

    private final JBTextField notBlankField;
    private final JBTextField notNullField;
    private final JBTextField sizeField;

    public ValidationMessageSettingDialog() {
        super(true);

        PropertiesComponent props = PropertiesComponent.getInstance();
        notBlankField = new JBTextField(props.getValue(NOTBLANK_KEY, "${name} 不得為空"));
        notNullField = new JBTextField(props.getValue(NOTNULL_KEY, "${name} 為必填"));
        sizeField = new JBTextField(props.getValue(SIZE_KEY, "${name} 長度不得超過${max}"));

        init();
        setTitle("驗證消息設置");
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // NotBlank消息
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("NotBlank消息:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(notBlankField, gbc);

        // NotNull消息
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("NotNull消息:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(notNullField, gbc);

        // Size消息
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Size消息:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(sizeField, gbc);

        // 提示說明
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(new JLabel("<html>可用的變量：<br>" +
                "${name} - 屬性名稱<br>" +
                "${comment} - 註解說明<br>" +
                "${max} - 最大長度（僅用於Size）<br><br>" +
                "範例：<br>" +
                "${name}(${comment}) 不得為空<br>" +
                "${comment}不得為空 (${name})</html>"), gbc);

        return panel;
    }

    @Override
    protected void doOKAction() {
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(NOTBLANK_KEY, notBlankField.getText());
        props.setValue(NOTNULL_KEY, notNullField.getText());
        props.setValue(SIZE_KEY, sizeField.getText());
        super.doOKAction();
    }

    public static String getNotBlankMessage(String propertyName, String comment) {
        String template = PropertiesComponent.getInstance().getValue(NOTBLANK_KEY, "${name} 不得為空");
        return template.replace("${name}", propertyName)
                .replace("${comment}", comment != null && !comment.isEmpty() ? comment : propertyName);
    }

    public static String getNotNullMessage(String propertyName, String comment) {
        String template = PropertiesComponent.getInstance().getValue(NOTNULL_KEY, "${name} 為必填");
        return template.replace("${name}", propertyName)
                .replace("${comment}", comment != null && !comment.isEmpty() ? comment : propertyName);
    }

    public static String getSizeMessage(String propertyName, String comment, String max) {
        String template = PropertiesComponent.getInstance().getValue(SIZE_KEY, "${name} 長度不得超過${max}");
        return template.replace("${name}", propertyName)
                .replace("${comment}", comment != null && !comment.isEmpty() ? comment : propertyName)
                .replace("${max}", max);
    }
}