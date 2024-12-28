package com.catchaybk.dtogeneratorplugin.ui;

import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiPackage;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DtoConfigDialog extends DialogWrapper {
    private static final String REMEMBERED_AUTHOR_KEY = "dto.generator.remembered.author";

    private final Project project;
    private JBTextField msgIdField;
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
    private JBTextField tranIdField;
    private JPanel tranIdPanel;
    private TextFieldWithBrowseButton packageChooser;
    private String initialPackage;

    public DtoConfigDialog(String msgId,
                           String author,
                           String mainClassName,
                           boolean isJava17,
                           boolean isUpstream,
                           Map<Integer, List<String>> levelTypesMap,
                           Project project,
                           String initialPackage) {
        super(true);
        this.project = project;
        this.initialMsgId = msgId;
        this.initialAuthor = author;
        this.initialMainClassName = mainClassName;
        this.initialJava17 = isJava17;
        this.isUpstream = isUpstream;
        this.levelTypesMap = levelTypesMap;
        this.initialPackage = initialPackage;
        init();
        setTitle("DTO Generator Configuration");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
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

        // 創建可滾動的類型配置面板
        JPanel typeConfigPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        addTypeConfigurations(typeConfigPanel, gbc, 0);

        // 為類型配置面板添加額外的底部空間
        JPanel spacer = new JPanel();
        gbc.gridx = 0;
        gbc.gridy = levelTypesMap.size() + 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        typeConfigPanel.add(spacer, gbc);

        // 創建滾動面板
        JBScrollPane scrollPane = new JBScrollPane(typeConfigPanel);
        scrollPane.setPreferredSize(new Dimension(450, 400));

        contentPanel.add(topPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        return contentPanel;
    }

    private void addBasicConfigurations(JPanel panel, GridBagConstraints gbc, int startRow) {
        int currentRow = startRow;

        // 包選擇器
        packageChooser = new TextFieldWithBrowseButton();
        packageChooser.setText(initialPackage);

        // 使用新的 PackageChooserDialog API
        packageChooser.addActionListener(e -> {
            PackageChooserDialog dialog = new PackageChooserDialog("選擇目標包", project);

            // 如果當前有值，預先選中
            String currentPackage = packageChooser.getText();
            if (!currentPackage.isEmpty()) {
                dialog.selectPackage(currentPackage);
            }

            // 使用 showAndGet() 替代 show()
            if (dialog.showAndGet()) {
                PsiPackage selectedPackage = dialog.getSelectedPackage();
                if (selectedPackage != null) {
                    packageChooser.setText(selectedPackage.getQualifiedName());
                }
            }
        });

        // 確保文本框可編輯
        packageChooser.getTextField().setEditable(true);

        addFormRow(panel, "目標包路徑:", packageChooser, gbc, currentRow++);


        // MSGID 配置
        msgIdField = new JBTextField(initialMsgId);
        msgIdField.getDocument().addDocumentListener(createDocumentListener());
        addFormRow(panel, "MSGID:", msgIdField, gbc, currentRow++);

        // 電文方向選擇
        messageDirectionComboBox = new JComboBox<>(new String[]{"無", "上行", "下行"});
        messageDirectionComboBox.setSelectedItem(getInitialMessageDirection());
        messageDirectionComboBox.addActionListener(e -> {
            updateTranIdVisibility();
            updateAllClassNames();
        });
        addFormRow(panel, "電文方向:", messageDirectionComboBox, gbc, currentRow++);

        // 電文ID配置面板
        tranIdPanel = new JPanel(new GridBagLayout());
        tranIdField = new JBTextField();
        tranIdField.getDocument().addDocumentListener(createDocumentListener());
        addFormRow(tranIdPanel, "電文ID:", tranIdField, gbc, 0);

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
        panel.add(rememberAuthorCheckBox, gbc);

        // Java版本選擇
        javaVersionComboBox = new JComboBox<>(new String[]{"Java 8", "Java 17"});
        javaVersionComboBox.setSelectedItem(initialJava17 ? "Java 17" : "Java 8");
        addFormRow(panel, "Java版本:", javaVersionComboBox, gbc, currentRow++);

        // 主類名配置
        mainClassField = new JBTextField(initialMainClassName);
        addFormRow(panel, "主類名:", mainClassField, gbc, currentRow++);

        addSeparator(panel, gbc, currentRow);
    }

    private void addFormRow(JPanel panel, String labelText, JComponent field, GridBagConstraints gbc, int row) {
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(100, 30));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
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
        gbc.insets = JBUI.insets(10, 0, 10, 0);
        panel.add(separator, gbc);
        gbc.gridwidth = 1;
        gbc.insets = JBUI.insets(5, 5, 5, 5);
    }

    private void addTypeConfigurations(JPanel panel, GridBagConstraints gbc, int startRow) {
        int row = startRow;
        for (Map.Entry<Integer, List<String>> entry : levelTypesMap.entrySet()) {
            int level = entry.getKey();
            List<String> types = entry.getValue();

            if (!types.isEmpty()) {
                JLabel levelLabel = new JLabel("第 " + level + " 層級類型配置");
                levelLabel.setFont(levelLabel.getFont().deriveFont(Font.BOLD));
                gbc.gridx = 0;
                gbc.gridy = row++;
                gbc.gridwidth = 2;
                panel.add(levelLabel, gbc);
                gbc.gridwidth = 1;

                for (String typeName : types) {
                    JBTextField classNameField = new JBTextField();
                    classNameFields.put(typeName, classNameField);
                    addFormRow(panel, "  " + typeName + ":", classNameField, gbc, row++);
                }

                addSeparator(panel, gbc, row++);
            }
        }
    }

    private DocumentListener createDocumentListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateAllClassNames();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateAllClassNames();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateAllClassNames();
            }
        };
    }

    private void updateAllClassNames() {
        String direction = (String) messageDirectionComboBox.getSelectedItem();
        String effectiveId = getEffectiveId();

        // 更新主類名
        String mainClassName = generateClassName(effectiveId, direction, "", true);
        mainClassField.setText(mainClassName);

        // 更新所有子類名
        for (Map.Entry<String, JBTextField> entry : classNameFields.entrySet()) {
            String baseName = entry.getKey();
            String newClassName = generateClassName(effectiveId, direction, baseName, false);
            entry.getValue().setText(newClassName);
        }
    }

    private String generateClassName(String msgId, String direction, String baseName, boolean isMainClass) {
        if (msgId == null || msgId.isEmpty() || "無".equals(direction)) {
            return isMainClass ? (baseName.isEmpty() ? "MainDTO" : baseName) : capitalizeFirstLetter(baseName);
        }

        String suffix = "上行".equals(direction) ? "Tranrq" : "Tranrs";
        String prefix = msgId.toUpperCase();
        return isMainClass ? prefix + suffix : prefix + suffix + capitalizeFirstLetter(baseName);
    }

    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private void updateTranIdVisibility() {
        String direction = (String) messageDirectionComboBox.getSelectedItem();
        boolean showTranId = !"無".equals(direction);
        tranIdPanel.setVisible(showTranId);

        if (!showTranId) {
            tranIdField.setText("");
        } else if (tranIdField.getText().isEmpty() && msgIdField.getText() != null) {
            tranIdField.setText(msgIdField.getText());
        }

        tranIdPanel.revalidate();
        tranIdPanel.repaint();
    }

    public String getTargetPackage() {
        return packageChooser.getText().trim();
    }

    public String getMsgId() {
        return msgIdField.getText().trim();
    }

    public String getAuthor() {
        return authorField.getText().trim();
    }

    public String getMainClassName() {
        return mainClassField.getText().trim();
    }

    public boolean isJava17() {
        return "Java 17".equals(javaVersionComboBox.getSelectedItem());
    }

    public String getClassName(String typeName) {
        JBTextField field = classNameFields.get(typeName);
        return field != null ? field.getText().trim() : "";
    }

    public String getMessageDirection() {
        return (String) messageDirectionComboBox.getSelectedItem();
    }

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

    private String getInitialMessageDirection() {
        return "無";
    }

    private String getEffectiveId() {
        String direction = getMessageDirection();
        return "無".equals(direction) ? msgIdField.getText().trim() : tranIdField.getText().trim();
    }

    @Override
    protected ValidationInfo doValidate() {
        if (mainClassField.getText().trim().isEmpty()) {
            return new ValidationInfo("Main class name cannot be empty", mainClassField);
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        if (rememberAuthorCheckBox.isSelected()) {
            PropertiesComponent.getInstance().setValue(REMEMBERED_AUTHOR_KEY, getAuthor());
        } else {
            PropertiesComponent.getInstance().unsetValue(REMEMBERED_AUTHOR_KEY);
        }
        super.doOKAction();
    }
}