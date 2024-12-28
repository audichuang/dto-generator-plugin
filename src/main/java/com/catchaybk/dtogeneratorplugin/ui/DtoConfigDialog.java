package com.catchaybk.dtogeneratorplugin.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DtoConfigDialog extends DialogWrapper {
    private static final String REMEMBERED_AUTHOR_KEY = "dto.generator.remembered.author";

    private JBTextField msgIdField; // 新增
    private JComboBox<String> messageDirectionComboBox;
    private JBTextField authorField;
    private JCheckBox rememberAuthorCheckBox;
    private JComboBox<String> javaVersionComboBox;
    private JBTextField mainClassField;
    private Map<String, JBTextField> classNameFields = new HashMap<>();
    private Map<Integer, List<String>> levelTypesMap;
    private String initialAuthor;
    private String initialMainClassName;
    private String initialMsgId;
    private boolean initialJava17;
    private boolean isUpstream;
    private JBTextField tranIdField; // 新增：用于输入电文ID
    private JPanel tranIdPanel;


    public DtoConfigDialog(String msgId, String author, String mainClassName, boolean isJava17,
                           boolean isUpstream, // 新增參數
                           Map<Integer, List<String>> levelTypesMap) {
        super(true);
        this.initialMsgId = msgId;
        this.initialAuthor = author;
        this.initialMainClassName = mainClassName;
        this.initialJava17 = isJava17;
        this.isUpstream = isUpstream; // 初始化
        this.levelTypesMap = levelTypesMap;
        init();
        setTitle("DTO Generator Configuration");
    }

    @Override
    protected JComponent createCenterPanel() {
        // 主容器使用 BorderLayout
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(10));

        // 創建頂部固定面板（基本配置）
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints topGbc = new GridBagConstraints();
        topGbc.fill = GridBagConstraints.HORIZONTAL;
        topGbc.insets = JBUI.insets(5, 5, 5, 5);
        topGbc.anchor = GridBagConstraints.WEST;

        // 添加基本配置到頂部面板
        addBasicConfigurations(topPanel, topGbc, 0);
        // 主類配置也放在頂部固定面板
        mainClassField = new JBTextField(initialMainClassName);
        addFormRow(topPanel, "主要類名:", mainClassField, topGbc, 6); // 基本配置後的下一行
        // 創建可滾動的類型配置面板
        JPanel typeConfigPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
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
                typeConfigPanel.add(levelLabel, gbc);
                gbc.gridwidth = 1;

                // 添加該層級的所有類型配置
                for (String typeName : types) {
                    JBTextField classNameField = new JBTextField();
                    classNameFields.put(typeName, classNameField);
                    addFormRow(typeConfigPanel, "  " + typeName + ":", classNameField, gbc, row++);
                }

                // 在每個層級後添加分隔符
                if (level < Collections.max(levelTypesMap.keySet())) {
                    addSeparator(typeConfigPanel, gbc, row++);
                }
            }
        }

        // 為類型配置面板添加額外的底部空間
        JPanel spacer = new JPanel();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        typeConfigPanel.add(spacer, gbc);

        // 創建滾動面板，只包含類型配置部分
        JBScrollPane scrollPane = new JBScrollPane(typeConfigPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // 設置首選大小
        scrollPane.setPreferredSize(new Dimension(450, 400));

        // 將固定面板放在頂部，滾動面板放在中間
        contentPanel.add(topPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        return contentPanel;
    }

    private void addBasicConfigurations(JPanel panel, GridBagConstraints gbc, int startRow) {
        int currentRow = startRow;

        // MSGID 配置
        msgIdField = new JBTextField(initialMsgId);
        addFormRow(panel, "MSGID:", msgIdField, gbc, currentRow++);

        // 電文方向選擇
        messageDirectionComboBox = new JComboBox<>(new String[]{"無", "上行", "下行"});
        messageDirectionComboBox.setSelectedItem(getInitialMessageDirection());
        messageDirectionComboBox.addActionListener(e -> {
            updateTranIdVisibility();
            updateAllClassNames();
        });
        addFormRow(panel, "電文方向:", messageDirectionComboBox, gbc, currentRow++);

        // 电文ID配置面板
        tranIdPanel = new JPanel(new GridBagLayout());
        tranIdField = new JBTextField();
        tranIdField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void textChanged(String newText) {
                updateAllClassNames();
            }
        });
        addFormRow(tranIdPanel, "上下行前綴:", tranIdField, gbc, 0);

        gbc.gridx = 0;
        gbc.gridy = currentRow++;
        gbc.gridwidth = 2;
        panel.add(tranIdPanel, gbc);
        gbc.gridwidth = 1;
        tranIdPanel.setVisible(false);

        // 作者配置
        authorField = new JBTextField(initialAuthor);
        addFormRow(panel, "作者:", authorField, gbc, currentRow++);

        // 記住作者選項
        rememberAuthorCheckBox = new JCheckBox("記住作者", !initialAuthor.isEmpty());
        gbc.gridx = 1;
        gbc.gridy = currentRow++;
        gbc.gridwidth = 1;
        panel.add(rememberAuthorCheckBox, gbc);

        // Java版本選擇
        javaVersionComboBox = new JComboBox<>(new String[]{"Java 8", "Java 17"});
        javaVersionComboBox.setSelectedItem(initialJava17 ? "Java 17" : "Java 8");
        addFormRow(panel, "Java版本:", javaVersionComboBox, gbc, currentRow++);

        // 添加分隔符
        addSeparator(panel, gbc, currentRow++);
    }


    private void addFormRow(JPanel panel, String labelText, JComponent field, GridBagConstraints gbc, int row) {
        // 標籤
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(100, 30)); // 減小標籤寬度
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE; // 標籤不需要填充
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(label, gbc);

        // 輸入欄位
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL; // 輸入欄位水平填充
        if (field instanceof JTextField) {
            field.setPreferredSize(new Dimension(300, 30));
        }
        panel.add(field, gbc);
    }


    private void addSeparator(JPanel panel, GridBagConstraints gbc, int row) {
        JSeparator separator = new JSeparator();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(10, 0, 10, 0); // 增加上下間距
        panel.add(separator, gbc);
        gbc.gridwidth = 1;
        gbc.insets = JBUI.insets(5, 5, 5, 5); // 恢復原始間距
    }


    private String generateClassName(String msgId, String direction, String baseName, boolean isMainClass) {
        if (msgId == null || msgId.isEmpty() || direction.equals("無")) {
            if (isMainClass) {
                return baseName.isEmpty() ? "MainDTO" : baseName;
            } else {
                // 確保 baseName 首字母大寫
                return capitalizeFirstLetter(baseName);
            }
        }

        String suffix = direction.equals("上行") ? "Tranrq" : "Tranrs";
        String prefix = msgId.toUpperCase();

        if (isMainClass) {
            return prefix + suffix;
        } else {
            // 確保 baseName 首字母大寫
            return prefix + suffix + capitalizeFirstLetter(baseName);
        }
    }

    // 添加一個輔助方法來處理首字母大寫
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }


    public String getEffectiveId() {
        String direction = getMessageDirection();
        if ("無".equals(direction)) {
            return msgIdField.getText().trim();
        }
        return tranIdField.getText().trim();
    }


    private void addTypeConfigurations(JPanel panel, GridBagConstraints gbc, int startRow) {
        for (Map.Entry<Integer, List<String>> entry : levelTypesMap.entrySet()) {
            int level = entry.getKey();
            List<String> types = entry.getValue();

            if (!types.isEmpty()) {
                JLabel levelLabel = new JLabel("第 " + level + " 層級類型配置");
                levelLabel.setFont(levelLabel.getFont().deriveFont(Font.BOLD));
                gbc.gridx = 0;
                gbc.gridy = startRow++;
                gbc.gridwidth = 2;
                panel.add(levelLabel, gbc);
                gbc.gridwidth = 1;

                for (String typeName : types) {
                    JBTextField classNameField = new JBTextField();
                    classNameField.setEditable(false); // 設為只讀，因為會自動生成
                    classNameFields.put(typeName, classNameField);
                    addFormRow(panel, "  " + typeName + ":", classNameField, gbc, startRow++);
                }
            }
        }
    }

    // 添加一個簡單的 DocumentListener 實現
    private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void textChanged(String newText);

        @Override
        default void insertUpdate(javax.swing.event.DocumentEvent e) {
            try {
                textChanged(e.getDocument().getText(0, e.getDocument().getLength()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        default void removeUpdate(javax.swing.event.DocumentEvent e) {
            try {
                textChanged(e.getDocument().getText(0, e.getDocument().getLength()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        default void changedUpdate(javax.swing.event.DocumentEvent e) {
            try {
                textChanged(e.getDocument().getText(0, e.getDocument().getLength()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    private void updateAllClassNames() {
        String direction = (String) messageDirectionComboBox.getSelectedItem();
        String effectiveId;

        if ("無".equals(direction)) {
            effectiveId = msgIdField.getText().trim();
        } else {
            effectiveId = tranIdField.getText().trim();
        }

        // 更新主类名
        String mainClassName = generateClassName(effectiveId, direction, "", true);
        mainClassField.setText(mainClassName);

        // 更新所有子类名
        for (Map.Entry<String, JBTextField> entry : classNameFields.entrySet()) {
            String baseName = entry.getKey();
            String newClassName = generateClassName(effectiveId, direction, baseName, false);
            entry.getValue().setText(newClassName);
        }
    }


    private void updateTranIdVisibility() {
        String direction = (String) messageDirectionComboBox.getSelectedItem();
        boolean showTranId = !"無".equals(direction);
        tranIdPanel.setVisible(showTranId);

        if (!showTranId) {
            tranIdField.setText(""); // 清空电文ID
        } else if (tranIdField.getText().isEmpty() && msgIdField.getText() != null) {
            // 如果电文ID为空且原始ID存在，则复制原始ID
            tranIdField.setText(msgIdField.getText());
        }

        // 触发面板重新布局
        tranIdPanel.revalidate();
        tranIdPanel.repaint();
    }


    public String getMsgId() {
        return msgIdField.getText().trim();
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

    public String getClassName(String typeName) {
        JBTextField field = classNameFields.get(typeName);
        return field != null ? field.getText().trim() : "";
    }

    private String getInitialMessageDirection() {

        return "無"; // 默認選擇"無"
    }

    /**
     * 獲取電文方向的註解文字
     *
     * @return 如果選擇"無"則返回空字符串,否則返回對應的電文方向說明
     */
    public String getMessageDirectionComment() {
        String direction = getMessageDirection();
        switch (direction) {
            case "上行":
                return "上行/請求電文";
            case "下行":
                return "下行/回應電文";
            default:
                return "";
        }
    }

    public String getMessageDirection() {
        return (String) messageDirectionComboBox.getSelectedItem();
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
