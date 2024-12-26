package com.catchaybk.dtogeneratorplugin.ui;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class DtoConfigDialog extends DialogWrapper {
    private final Map<Integer, JTextField> levelClassNames = new HashMap<>();
    private JTextField authorField;
    private final int maxLevel;

    public DtoConfigDialog(int maxLevel) {
        super(true);
        this.maxLevel = maxLevel;
        init();
        setTitle("DTO Configuration");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 添加作者欄位
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("作者:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        authorField = new JTextField(20);
        panel.add(authorField, gbc);

        // 為每一層添加類名輸入欄位
        for (int i = 1; i <= maxLevel; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            panel.add(new JLabel("Level " + i + " 類名:"), gbc);

            gbc.gridx = 1;
            JTextField classNameField = new JTextField(20);
            panel.add(classNameField, gbc);
            levelClassNames.put(i, classNameField);
        }

        return panel;
    }

    public String getAuthor() {
        return authorField.getText().trim();
    }

    public String getClassName(int level) {
        JTextField field = levelClassNames.get(level);
        return field != null ? field.getText().trim() : "";
    }
}