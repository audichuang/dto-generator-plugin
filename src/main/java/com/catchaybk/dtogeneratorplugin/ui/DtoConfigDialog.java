package com.catchaybk.dtogeneratorplugin.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class DtoConfigDialog extends DialogWrapper {
    private static final String REMEMBERED_AUTHOR_KEY = "dto.generator.remembered.author";

    private JBTextField authorField;
    private JCheckBox rememberAuthorCheckBox;
    private JComboBox<String> javaVersionComboBox;
    private JBTextField mainClassField;
    private final Map<Integer, JBTextField> levelClassFields = new HashMap<>();

    private final int maxLevel;
    private String initialAuthor;
    private String initialMainClassName;
    private boolean initialJava17;

    public DtoConfigDialog(int maxLevel, String author, String mainClassName, boolean isJava17) {
        super(true);
        this.maxLevel = maxLevel;
        this.initialAuthor = author;
        this.initialMainClassName = mainClassName;
        this.initialJava17 = isJava17;
        init();
        setTitle("DTO Generator Configuration");
    }

    @Override
    protected JComponent createCenterPanel() {
        // 主面板
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // 作者欄位
        authorField = new JBTextField(initialAuthor);
        addFormRow(mainPanel, "作者:", authorField, gbc, row++);

        // 記住作者選項
        rememberAuthorCheckBox = new JCheckBox("記住作者", !initialAuthor.isEmpty());
        gbc.gridx = 1;
        gbc.gridy = row++;
        mainPanel.add(rememberAuthorCheckBox, gbc);

        // Java版本選擇
        javaVersionComboBox = new JComboBox<>(new String[]{"Java 8", "Java 17"});
        javaVersionComboBox.setSelectedItem(initialJava17 ? "Java 17" : "Java 8");
        addFormRow(mainPanel, "Java版本:", javaVersionComboBox, gbc, row++);

        // 添加分隔線
        addSeparator(mainPanel, gbc, row++);

        // 主要類名
        mainClassField = new JBTextField(initialMainClassName);
        addFormRow(mainPanel, "主要類名:", mainClassField, gbc, row++);

        // 第一層級類名
        JBTextField level1Field = new JBTextField();
        levelClassFields.put(1, level1Field);
        addFormRow(mainPanel, "第一層級 (SupList):", level1Field, gbc, row++);

        // 第二層級類名
        JBTextField level2Field = new JBTextField();
        levelClassFields.put(2, level2Field);
        addFormRow(mainPanel, "第二層級 (SubSeqnoList):", level2Field, gbc, row++);

        // 設置首選大小
        mainPanel.setPreferredSize(new Dimension(450, row * 40));

        return mainPanel;
    }

    private void addFormRow(JPanel panel, String labelText, JComponent field,
                            GridBagConstraints gbc, int row) {
        // 標籤
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(150, 30));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        panel.add(label, gbc);

        // 輸入欄位
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        if (field instanceof JTextField) {
            field.setPreferredSize(new Dimension(250, 30));
        }
        panel.add(field, gbc);
    }

    private void addSeparator(JPanel panel, GridBagConstraints gbc, int row) {
        JSeparator separator = new JSeparator();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(separator, gbc);
        gbc.gridwidth = 1; // 重置gridwidth
    }

    // Getter方法
    public String getAuthor() {
        return authorField.getText().trim();
    }

    public boolean isRememberAuthor() {
        return rememberAuthorCheckBox.isSelected();
    }

    public boolean isJava17() {
        return "Java 17".equals(javaVersionComboBox.getSelectedItem());
    }

    public String getMainClassName() {
        return mainClassField.getText().trim();
    }

    public String getClassName(int level) {
        JBTextField field = levelClassFields.get(level);
        return field != null ? field.getText().trim() : "";
    }

    @Override
    protected void doOKAction() {
        if (isRememberAuthor()) {
            PropertiesComponent.getInstance().setValue(REMEMBERED_AUTHOR_KEY, getAuthor());
        } else {
            PropertiesComponent.getInstance().unsetValue(REMEMBERED_AUTHOR_KEY);
        }
        super.doOKAction();
    }
}
