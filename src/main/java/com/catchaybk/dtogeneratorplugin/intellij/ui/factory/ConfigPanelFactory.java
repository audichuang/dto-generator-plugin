package com.catchaybk.dtogeneratorplugin.intellij.ui.factory;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * 配置面板工廠類
 * 負責創建各種配置面板
 */
public class ConfigPanelFactory {
    private static final int LABEL_WIDTH = 130;
    private static final int FIELD_HEIGHT = 36;
    private static final int SECTION_SPACING = 20;
    private static final Color HEADER_BACKGROUND = new JBColor(new Color(240, 245, 250), new Color(43, 45, 48));
    private static final Color LABEL_BACKGROUND = new JBColor(new Color(245, 248, 250), new Color(43, 43, 43));
    private static final Color SEPARATOR_COLOR = new JBColor(new Color(218, 220, 224), new Color(60, 63, 65));
    private static final Color FOCUS_COLOR = new JBColor(new Color(24, 115, 204), new Color(75, 110, 175));

    /**
     * 創建基本配置面板
     */
    public static JPanel createBasicConfigPanel(JComponent[] components, String[] labels) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(15, 10, 20, 10));

        // 創建標題
        JLabel titleLabel = new JBLabel("基本配置");
        titleLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD, 15f));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(HEADER_BACKGROUND);
        titlePanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, SEPARATOR_COLOR),
                JBUI.Borders.empty(10, 15)));
        titlePanel.add(titleLabel, BorderLayout.WEST);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(titlePanel, gbc);

        // 添加表單元素
        gbc = createDefaultConstraints();
        for (int i = 0; i < components.length; i++) {
            addFormRow(panel, labels[i], components[i], gbc, i + 1, i < 3 || i == 4 || i == 6);
        }

        return panel;
    }

    /**
     * 創建類型配置面板
     */
    public static JPanel createTypeConfigPanel(Map<Integer, List<String>> levelTypesMap,
            Map<String, JBTextField> classNameFields) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(5, 10, 15, 10));

        // 創建標題
        JLabel titleLabel = new JBLabel("類型配置");
        titleLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD, 15f));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(HEADER_BACKGROUND);
        titlePanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, SEPARATOR_COLOR),
                JBUI.Borders.empty(10, 15)));

        // 添加說明文字
        JPanel titleContentPanel = new JPanel(new BorderLayout());
        titleContentPanel.setBackground(HEADER_BACKGROUND);
        titleContentPanel.add(titleLabel, BorderLayout.WEST);

        JLabel helpLabel = new JBLabel("<html><font color='gray' size='2'>以下配置用於自訂生成的DTO類名</font></html>");
        helpLabel.setBorder(JBUI.Borders.emptyLeft(10));
        titleContentPanel.add(helpLabel, BorderLayout.EAST);

        titlePanel.add(titleContentPanel, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(titlePanel, gbc);

        // 添加各層級配置
        gbc = createDefaultConstraints();
        int row = 1;

        for (Map.Entry<Integer, List<String>> entry : levelTypesMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                row = addLevelConfiguration(panel, gbc, row, entry.getKey(), entry.getValue(), classNameFields);
            }
        }

        addBottomSpacer(panel, gbc, row);
        return panel;
    }

    private static GridBagConstraints createDefaultConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private static void addFormRow(JPanel panel, String labelText, JComponent field,
            GridBagConstraints gbc, int row, boolean required) {
        if (labelText != null && !labelText.isEmpty()) {
            JPanel labelPanel = new JPanel(new BorderLayout());
            labelPanel.setOpaque(false);

            JLabel label = new JBLabel(labelText);
            label.setPreferredSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));
            label.setFont(UIUtil.getLabelFont());
            labelPanel.add(label, BorderLayout.WEST);

            if (required) {
                JLabel requiredLabel = new JBLabel(" *");
                requiredLabel.setForeground(JBColor.RED);
                labelPanel.add(requiredLabel, BorderLayout.EAST);
            }

            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            panel.add(labelPanel, gbc);
        }

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        if (field instanceof JTextField) {
            field.setPreferredSize(new Dimension(300, FIELD_HEIGHT));

            // 保持現有的邊框樣式
        } else if (field instanceof JComboBox) {
            field.setPreferredSize(new Dimension(300, FIELD_HEIGHT));
        }

        panel.add(field, gbc);
    }

    private static void addFormRow(JPanel panel, String labelText, JComponent field,
            GridBagConstraints gbc, int row) {
        addFormRow(panel, labelText, field, gbc, row, false);
    }

    private static int addLevelConfiguration(JPanel panel, GridBagConstraints gbc,
            int startRow, int level, List<String> types, Map<String, JBTextField> classNameFields) {
        int row = startRow;

        addLevelHeader(panel, gbc, row++, level);

        for (String typeName : types) {
            JBTextField field = new JBTextField();

            // 創建現代風格的文字框
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SEPARATOR_COLOR, 1),
                    JBUI.Borders.empty(6, 8)));

            // 添加焦點邊框效果
            field.addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent evt) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            new LineBorder(FOCUS_COLOR, 1),
                            JBUI.Borders.empty(6, 8)));
                }

                public void focusLost(java.awt.event.FocusEvent evt) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(SEPARATOR_COLOR, 1),
                            JBUI.Borders.empty(6, 8)));
                }
            });

            classNameFields.put(typeName, field);

            // 設置提示文字
            field.setToolTipText("為 " + typeName + " 類型設置自定義類名");

            // 使用縮進來顯示層級關係
            addFormRow(panel, "  " + typeName + ":", field, gbc, row++);
        }

        addSeparator(panel, gbc, row++);
        return row;
    }

    private static void addLevelHeader(JPanel panel, GridBagConstraints gbc, int row, int level) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(LABEL_BACKGROUND);
        headerPanel.setBorder(JBUI.Borders.empty(8, 12));

        JLabel header = new JLabel("第 " + level + " 層級類型配置");
        header.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD, 14f));
        headerPanel.add(header, BorderLayout.WEST);

        // 添加說明文字
        JLabel helpLabel = new JBLabel("<html><font color='gray' size='2'>設置層級 " + level + " 的類型命名方式</font></html>");
        headerPanel.add(helpLabel, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = JBUI.insets(12, 0, 8, 0);
        panel.add(headerPanel, gbc);

        // 重置insets
        gbc.insets = JBUI.insets(8, 10, 8, 10);
        gbc.gridwidth = 1;
    }

    private static void addSeparator(JPanel panel, GridBagConstraints gbc, int row) {
        JSeparator separator = new JSeparator();
        separator.setForeground(SEPARATOR_COLOR);
        separator.setBackground(UIUtil.getPanelBackground());

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = JBUI.insets(SECTION_SPACING / 2, 0, SECTION_SPACING / 2, 0);
        panel.add(separator, gbc);

        // 重置insets
        gbc.insets = JBUI.insets(8, 10, 8, 10);
        gbc.gridwidth = 1;
    }

    private static void addBottomSpacer(JPanel panel, GridBagConstraints gbc, int row) {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(spacer, gbc);
    }
}