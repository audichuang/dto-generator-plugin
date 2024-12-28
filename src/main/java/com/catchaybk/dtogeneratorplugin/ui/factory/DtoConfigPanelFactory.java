package com.catchaybk.dtogeneratorplugin.ui.factory;

import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * DTO配置面板工廠類
 * 負責創建各種配置面板
 */
public class DtoConfigPanelFactory {
    private static final int LABEL_WIDTH = 100;
    private static final int FIELD_HEIGHT = 30;

    /**
     * 創建基本配置面板
     */
    public static JPanel createBasicConfigPanel(JComponent[] components, String[] labels) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createDefaultConstraints();

        for (int i = 0; i < components.length; i++) {
            addFormRow(panel, labels[i], components[i], gbc, i);
        }

        return panel;
    }

    /**
     * 創建類型配置面板
     */
    public static JPanel createTypeConfigPanel(Map<Integer, List<String>> levelTypesMap,
            Map<String, JBTextField> classNameFields) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createDefaultConstraints();
        int row = 0;

        for (Map.Entry<Integer, List<String>> entry : levelTypesMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                row = addLevelConfiguration(panel, gbc, row, entry.getKey(), entry.getValue(), classNameFields);
            }
        }

        addBottomSpacer(panel, gbc, levelTypesMap.size() + 1);
        return panel;
    }

    private static GridBagConstraints createDefaultConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private static void addFormRow(JPanel panel, String labelText, JComponent field,
            GridBagConstraints gbc, int row) {
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        if (field instanceof JTextField) {
            field.setPreferredSize(new Dimension(300, FIELD_HEIGHT));
        }
        panel.add(field, gbc);
    }

    private static int addLevelConfiguration(JPanel panel, GridBagConstraints gbc,
            int startRow, int level, List<String> types, Map<String, JBTextField> classNameFields) {
        int row = startRow;

        addLevelHeader(panel, gbc, row++, level);

        for (String typeName : types) {
            JBTextField field = new JBTextField();
            classNameFields.put(typeName, field);
            addFormRow(panel, "  " + typeName + ":", field, gbc, row++);
        }

        addSeparator(panel, gbc, row++);
        return row;
    }

    private static void addLevelHeader(JPanel panel, GridBagConstraints gbc, int row, int level) {
        JLabel header = new JLabel("第 " + level + " 層級類型配置");
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(header, gbc);
        gbc.gridwidth = 1;
    }

    private static void addSeparator(JPanel panel, GridBagConstraints gbc, int row) {
        JSeparator separator = new JSeparator();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(10, 0, 10, 0);
        panel.add(separator, gbc);
        gbc.gridwidth = 1;
        gbc.insets = JBUI.insets(5, 5, 5, 5);
    }

    private static void addBottomSpacer(JPanel panel, GridBagConstraints gbc, int row) {
        JPanel spacer = new JPanel();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(spacer, gbc);
    }
}