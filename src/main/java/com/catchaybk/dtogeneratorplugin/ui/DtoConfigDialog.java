package com.catchaybk.dtogeneratorplugin.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class DtoConfigDialog extends DialogWrapper {
    private static final String REMEMBERED_AUTHOR_KEY = "dto.generator.remembered.author";

    private JBTextField authorField;
    private JCheckBox rememberAuthorCheckBox;
    private JComboBox<String> javaVersionComboBox;
    private JBTextField mainClassField;
    private Map<String, JBTextField> classNameFields = new HashMap<>();
    private Map<Integer, List<String>> levelTypesMap; // 按層級組織的類型
    private String initialAuthor;
    private String initialMainClassName;
    private boolean initialJava17;

    public DtoConfigDialog(String author, String mainClassName, boolean isJava17,
                         Map<Integer, List<String>> levelTypesMap) {
        super(true);
        this.initialAuthor = author;
        this.initialMainClassName = mainClassName;
        this.initialJava17 = isJava17;
        this.levelTypesMap = levelTypesMap;
        init();
        setTitle("DTO Generator Configuration");
    }

    @Override
    protected JComponent createCenterPanel() {
        // 創建主容器面板（使用BorderLayout）
        JPanel contentPanel = new JPanel(new BorderLayout());

        // 創建一個面板來包含所有配置項（使用GridBagLayout）
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // 基本配置部分
        addBasicConfigurations(mainPanel, gbc, row);
        row += 4; // 基本配置佔用3行 + 1行分隔符
        // 主類配置
        mainClassField = new JBTextField(initialMainClassName);
        addFormRow(mainPanel, "主要類名:", mainClassField, gbc, row++);
            addSeparator(mainPanel, gbc, row++);

        // 按層級添加類型配置
        for (Map.Entry<Integer, List<String>> entry : levelTypesMap.entrySet()) {
            int level = entry.getKey();
            List<String> types = entry.getValue();

            if (!types.isEmpty()) {
                // 添加層級標題
                JLabel levelLabel = new JLabel("第 " + level + " 層級類型配置");
                levelLabel.setFont(levelLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
                gbc.gridy = row++;
        gbc.gridwidth = 2;
                mainPanel.add(levelLabel, gbc);
        gbc.gridwidth = 1;

                // 添加該層級的所有類型配置
                for (String typeName : types) {
                    JBTextField classNameField = new JBTextField();
                    classNameFields.put(typeName, classNameField);
                    addFormRow(mainPanel, "  " + typeName + ":", classNameField, gbc, row++);
    }

                // 在每個層級後添加分隔符
                if (level < Collections.max(levelTypesMap.keySet())) {
                    addSeparator(mainPanel, gbc, row++);
    }
    }
    }

        // 創建滾動面板
        JBScrollPane scrollPane = new JBScrollPane(mainPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // 設置首選大小
        scrollPane.setPreferredSize(new Dimension(450, 500));

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        return contentPanel;
    }

    private void addBasicConfigurations(JPanel panel, GridBagConstraints gbc, int startRow) {
        // 作者配置
        authorField = new JBTextField(initialAuthor);
        addFormRow(panel, "作者:", authorField, gbc, startRow);

        // 記住作者選項
        rememberAuthorCheckBox = new JCheckBox("記住作者", !initialAuthor.isEmpty());
        gbc.gridx = 1;
        gbc.gridy = startRow + 1;
        panel.add(rememberAuthorCheckBox, gbc);

        // Java版本選擇
        javaVersionComboBox = new JComboBox<>(new String[]{"Java 8", "Java 17"});
        javaVersionComboBox.setSelectedItem(initialJava17 ? "Java 17" : "Java 8");
        addFormRow(panel, "Java版本:", javaVersionComboBox, gbc, startRow + 2);

        // 添加分隔符
        addSeparator(panel, gbc, startRow + 3);
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
        gbc.gridwidth = 1;
    }

    // Getter方法保持不變
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

    public String getClassName(String typeName) {
        JBTextField field = classNameFields.get(typeName);
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
