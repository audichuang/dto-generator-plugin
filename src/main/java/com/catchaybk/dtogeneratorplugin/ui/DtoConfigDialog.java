package com.catchaybk.dtogeneratorplugin.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class DtoConfigDialog extends DialogWrapper {
    private static final String REMEMBERED_AUTHOR_KEY = "dto.generator.remembered.author";

    private JTextField authorField;
    private JCheckBox rememberAuthorCheckBox;
    private JComboBox<String> javaVersionComboBox;
    private JTextField mainClassField;
    private final Map<Integer, JTextField> levelClassFields = new HashMap<>();

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
        setTitle("DTO Configuration");
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridwidth = 2;
        int row = 0;

        // 1. 作者設置
        addLabelAndField(panel, "作者:", authorField = new JTextField(initialAuthor, 20), gbc, row++);

        // 2. 記住作者選項
        rememberAuthorCheckBox = new JCheckBox("記住作者", !initialAuthor.isEmpty());
        gbc.gridy = row++;
        panel.add(rememberAuthorCheckBox, gbc);

        // 3. Java 版本選擇
        addLabelAndField(panel, "Java版本:",
                javaVersionComboBox = new JComboBox<>(new String[]{"Java 8", "Java 17"}), gbc, row++);
        javaVersionComboBox.setSelectedItem(initialJava17 ? "Java 17" : "Java 8");

        // 4. 主要類名
        addLabelAndField(panel, "主要類名:",
                mainClassField = new JTextField(initialMainClassName, 20), gbc, row++);

        // 5. 第一層級類名（SupList）
        addLabelAndField(panel, "第一層級類名 (SupList):",
                levelClassFields.computeIfAbsent(1, k -> new JTextField(20)), gbc, row++);

        // 6. 第二層級類名（SubSeqnoList）
        addLabelAndField(panel, "第二層級類名 (SubSeqnoList):",
                levelClassFields.computeIfAbsent(2, k -> new JTextField(20)), gbc, row++);

        // 創建滾動面板
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(500, Math.min(500, row * 35)));
        return scrollPane;
    }

    private void addLabelAndField(JPanel panel, String labelText, JComponent field,
                                  GridBagConstraints gbc, int row) {
        gbc.gridy = row;
        panel.add(new JLabel(labelText), gbc);
        panel.add(field, gbc);
    }

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
        JTextField field = levelClassFields.get(level);
        return field != null ? field.getText().trim() : "";
    }

    @Override
    protected void doOKAction() {
        // 保存作者信息
        if (isRememberAuthor()) {
            PropertiesComponent.getInstance().setValue(REMEMBERED_AUTHOR_KEY, getAuthor());
        } else {
            PropertiesComponent.getInstance().unsetValue(REMEMBERED_AUTHOR_KEY);
        }
        super.doOKAction();
    }
}
