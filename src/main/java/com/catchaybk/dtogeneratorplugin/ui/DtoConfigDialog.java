package com.catchaybk.dtogeneratorplugin.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DtoConfigDialog extends DialogWrapper {
    private static final String REMEMBERED_AUTHOR_KEY = "dto.generator.remembered.author";

    private JBTextField authorField;
    private JCheckBox rememberAuthorCheckBox;
    private JComboBox<String> javaVersionComboBox;
    private JBTextField mainClassField;
    private Map<String, JBTextField> classNameFields = new HashMap<>();
    private List<String> customTypes;
    private String initialAuthor;
    private String initialMainClassName;
    private boolean initialJava17;

    public DtoConfigDialog(String author, String mainClassName, boolean isJava17, List<String> customTypes) {
        super(true);
        this.initialAuthor = author;
        this.initialMainClassName = mainClassName;
        this.initialJava17 = isJava17;
        this.customTypes = customTypes;
        init();
        setTitle("DTO Generator Configuration");
    }

    @Override
    protected JComponent createCenterPanel() {
        // 創建主容器面板
        JPanel contentPanel = new JPanel(new BorderLayout());

        // 創建內容面板
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // 基本配置部分
        authorField = new JBTextField(initialAuthor);
        addFormRow(mainPanel, "作者:", authorField, gbc, row++);

        rememberAuthorCheckBox = new JCheckBox("記住作者", !initialAuthor.isEmpty());
        gbc.gridx = 1;
        gbc.gridy = row++;
        mainPanel.add(rememberAuthorCheckBox, gbc);

        javaVersionComboBox = new JComboBox<>(new String[]{"Java 8", "Java 17"});
        javaVersionComboBox.setSelectedItem(initialJava17 ? "Java 17" : "Java 8");
        addFormRow(mainPanel, "Java版本:", javaVersionComboBox, gbc, row++);

        addSeparator(mainPanel, gbc, row++);

        // 主類配置
        mainClassField = new JBTextField(initialMainClassName);
        addFormRow(mainPanel, "主要類名:", mainClassField, gbc, row++);

        // 為每個自定義類型添加配置欄位
        if (!customTypes.isEmpty()) {
            addSeparator(mainPanel, gbc, row++);
            for (String typeName : customTypes) {
                JBTextField classNameField = new JBTextField();
                classNameFields.put(typeName, classNameField);
                addFormRow(mainPanel, typeName + " 類名:", classNameField, gbc, row++);
            }
        }

        // 創建滾動面板
        JBScrollPane scrollPane = new JBScrollPane(mainPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // 設置首選大小
        scrollPane.setPreferredSize(new Dimension(450, 500));  // 固定高度，允許滾動

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        return contentPanel;
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
