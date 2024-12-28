package com.catchaybk.dtogeneratorplugin.ui;

import com.catchaybk.dtogeneratorplugin.generator.DtoNameGenerator;
import com.catchaybk.dtogeneratorplugin.model.DtoConfigData;
import com.catchaybk.dtogeneratorplugin.ui.factory.DtoConfigPanelFactory;
import com.catchaybk.dtogeneratorplugin.validator.DtoConfigValidator;
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
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO配置對話框
 * 用於配置DTO生成的相關參數，包括：
 * 1. 基本配置（包路徑、作者、Java）
 * 2. 電文相關配置（MSGID、電文方向等）
 * 3. 類名配置（主類名和各層級類名）
 */
public class DtoConfigDialog extends DialogWrapper {
    // 常量定義
    private static final String TITLE = "DTO Generator Configuration";
    private static final String REMEMBERED_AUTHOR_KEY = "dto.generator.remembered.author";
    private static final String[] MESSAGE_DIRECTIONS = { "無", "上行", "下行" };
    private static final String[] JAVA_VERSIONS = { "Java 8", "Java 17" };
    private static final int LABEL_WIDTH = 150;
    private static final int FIELD_HEIGHT = 30;
    private static final int SCROLL_WIDTH = 600;
    private static final int SCROLL_HEIGHT = 600;

    // UI組件
    private final UIComponents ui;

    // 配置數據
    private final ConfigData config;
    private final Project project;
    private final Map<String, JBTextField> classNameFields = new HashMap<>();
    private final Map<Integer, List<String>> levelTypesMap;

    private static final String[] JSON_STYLES = {
            "原始格式 (studentName -> studentName)",
            "全大寫 (studentName -> STUDENTNAME)",
            "全小寫 (studentName -> studentname)",
            "大寫底線 (studentName -> STUDENT_NAME)",
            "小駝峰 (StudentName -> studentName)",
            "大駝峰 (studentName -> StudentName)"
    };

    private static final String[] JSON_ALIAS_OPTIONS = {
            "無 (不添加 JsonAlias)",
            "原始格式 (studentName -> studentName)",
            "全大寫 (studentName -> STUDENTNAME)",
            "全小寫 (studentName -> studentname)",
            "大寫底線 (studentName -> STUDENT_NAME)",
            "小駝峰 (StudentName -> studentName)",
            "大駝峰 (studentName -> StudentName)"
    };

    public DtoConfigDialog(String msgId, String author, String mainClassName,
            boolean isJava17, boolean isUpstream, Map<Integer, List<String>> levelTypesMap,
            Project project, String initialPackage) {
        super(true);
        this.project = project;
        this.levelTypesMap = levelTypesMap;
        this.config = new ConfigData(msgId, author, mainClassName, isJava17, isUpstream, initialPackage);
        this.ui = new UIComponents();

        init();
        setTitle(TITLE);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBorder(JBUI.Borders.empty(10));

        // 創建基本配置板
        JComponent[] components = {
                ui.packageChooser,
                ui.msgIdField,
                ui.directionComboBox,
                ui.tranIdPanel,
                ui.authorField,
                ui.javaVersionBox,
                ui.mainClassField,
                ui.jsonPropertyStyleCombo,
                ui.jsonAliasPanel
        };

        String[] labels = {
                "目標包路徑:",
                "MSGID:",
                "電文方向:",
                "",
                "作者:",
                "Java版本:",
                "主類名:",
                "JSON Property 格式:",
                "JSON Alias 格式:"
        };

        // 創建基本配置面板
        JPanel basicPanel = DtoConfigPanelFactory.createBasicConfigPanel(components, labels);

        // 初始化電文ID面板
        addFormRow(ui.tranIdPanel, "電文ID:", ui.tranIdField, createDefaultConstraints(), 0);
        ui.tranIdPanel.setVisible(false);

        // 創建類型配置面板
        JPanel configPanel = DtoConfigPanelFactory.createTypeConfigPanel(levelTypesMap, classNameFields);

        // 使用 BoxLayout 來控制垂直布局
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(basicPanel);
        contentPanel.add(Box.createVerticalStrut(10)); // 添加固定間距
        contentPanel.add(configPanel);

        // 將整個內容放入滾動面板
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setPreferredSize(new Dimension(SCROLL_WIDTH, SCROLL_HEIGHT));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    /**
     * 配置數據類
     * 存儲所有配置相關的初始值
     */
    private static class ConfigData {
        final String msgId;
        final String author;
        final String mainClassName;
        final boolean isJava17;
        final boolean isUpstream;
        final String initialPackage;

        ConfigData(String msgId, String author, String mainClassName,
                boolean isJava17, boolean isUpstream, String initialPackage) {
            this.msgId = msgId;
            this.author = author;
            this.mainClassName = mainClassName;
            this.isJava17 = isJava17;
            this.isUpstream = isUpstream;
            this.initialPackage = initialPackage;
        }
    }

    // Getter方法
    public String getTargetPackage() {
        return ui.packageChooser.getText().trim();
    }

    public String getMsgId() {
        return ui.msgIdField.getText().trim();
    }

    public String getAuthor() {
        return ui.authorField.getText().trim();
    }

    public String getMainClassName() {
        return ui.mainClassField.getText().trim();
    }

    public boolean isJava17() {
        return "Java 17".equals(ui.javaVersionBox.getSelectedItem());
    }

    public String getClassName(String typeName) {
        JBTextField field = classNameFields.get(typeName);
        return field != null ? field.getText().trim() : "";
    }

    /**
     * 創建默認的GridBagConstraints
     *
     * @return 配置好的GridBagConstraints實例
     */
    private GridBagConstraints createDefaultConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    /**
     * 添加底部填充空間
     *
     * @param panel 要添加空間的面板
     * @param gbc   GridBag約束
     */
    private void addBottomSpacer(JPanel panel, GridBagConstraints gbc) {
        JPanel spacer = new JPanel();
        gbc.gridx = 0;
        gbc.gridy = levelTypesMap.size() + 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(spacer, gbc);
    }

    /**
     * 添加表單行
     *
     * @param panel     目標面板
     * @param labelText 標籤文本
     * @param field     輸入組件
     * @param gbc       GridBag約束
     * @param row       行號
     */
    private void addFormRow(JPanel panel, String labelText, JComponent field, GridBagConstraints gbc, int row) {
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
            field.setPreferredSize(new Dimension(400, FIELD_HEIGHT));
        }
        panel.add(field, gbc);
    }

    /**
     * 添加分隔線
     *
     * @param panel 目標面板
     * @param gbc   GridBag約束
     * @param row   行號
     */
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

    /**
     * 創建帶有文檔監聽器的文本框
     *
     * @param initialText 初始文本
     * @return 配置好的文本框
     */
    private JBTextField createTextField(String initialText) {
        JBTextField field = new JBTextField(initialText);
        field.getDocument().addDocumentListener(createDocumentListener());
        return field;
    }

    /**
     * 顯示包選擇器對話框
     *
     * @param packageChooser 包選擇器組件
     */
    private void showPackageChooserDialog(TextFieldWithBrowseButton packageChooser) {
        PackageChooserDialog dialog = new PackageChooserDialog("選擇目標包", project);
        String currentPackage = packageChooser.getText();
        if (!currentPackage.isEmpty()) {
            dialog.selectPackage(currentPackage);
        }
        if (dialog.showAndGet()) {
            PsiPackage selectedPackage = dialog.getSelectedPackage();
            if (selectedPackage != null) {
                packageChooser.setText(selectedPackage.getQualifiedName());
            }
        }
    }

    /**
     * 添加電文ID面板
     *
     * @param panel 目標面板
     * @param gbc   GridBag約束
     * @param row   行號
     */
    private void addTranIdPanel(JPanel panel, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(ui.tranIdPanel, gbc);
        gbc.gridwidth = 1;
        addFormRow(ui.tranIdPanel, "電文ID:", ui.tranIdField, createDefaultConstraints(), 0);
        ui.tranIdPanel.setVisible(false);
    }

    /**
     * 添加作者配置區域
     *
     * @param panel 目標面板
     * @param gbc   GridBag約束
     * @param row   行號
     */
    private void addAuthorConfig(JPanel panel, GridBagConstraints gbc, int row) {
        addFormRow(panel, "作者:", ui.authorField, gbc, row);
        gbc.gridx = 1;
        gbc.gridy = row + 1;
        panel.add(ui.rememberAuthorBox, gbc);
    }

    /**
     * 添加版本和主類配置區域
     *
     * @param panel 目標面板
     * @param gbc   GridBag約束
     * @param row   行號
     */
    private void addVersionAndMainClass(JPanel panel, GridBagConstraints gbc, int row) {
        addFormRow(panel, "Java版本:", ui.javaVersionBox, gbc, row);
        addFormRow(panel, "主類名:", ui.mainClassField, gbc, row + 1);
        addSeparator(panel, gbc, row + 2);
    }

    /**
     * 創建文檔變更監聽器
     *
     * @return 文檔監聽器實例
     */
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

    /**
     * 更新所有類名
     * 當MSGID或電文方向改變時自動更新所有相關���名
     */
    private void updateAllClassNames() {
        String direction = (String) ui.directionComboBox.getSelectedItem();
        String effectiveId = getEffectiveId();

        // 更新主類名
        String mainClassName = DtoNameGenerator.generateClassName(
                effectiveId, direction, "", true);
        ui.mainClassField.setText(mainClassName);

        // 更新所有子類名
        for (Map.Entry<String, JBTextField> entry : classNameFields.entrySet()) {
            String baseName = entry.getKey();
            String newClassName = DtoNameGenerator.generateClassName(
                    effectiveId, direction, baseName, false);
            entry.getValue().setText(newClassName);
        }
    }

    /**
     * 生成類名
     *
     * @param msgId       消息ID
     * @param direction   電文方向
     * @param baseName    基礎名稱
     * @param isMainClass 是否為主類
     * @return 生成的類名
     */
    private String generateClassName(String msgId, String direction, String baseName, boolean isMainClass) {
        if (msgId == null || msgId.isEmpty() || "無".equals(direction)) {
            return isMainClass ? (baseName.isEmpty() ? "MainDTO" : baseName) : capitalizeFirstLetter(baseName);
        }

        String suffix = "上行".equals(direction) ? "Tranrq" : "Tranrs";
        String prefix = msgId.toUpperCase();
        return isMainClass ? prefix + suffix : prefix + suffix + capitalizeFirstLetter(baseName);
    }

    /**
     * 首字母大寫
     *
     * @param input 輸入字符串
     * @return 首字母大寫後的字符串
     */
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    /**
     * 更新電文ID面板的可見性
     * 根據電文方向決定是否顯示電文ID輸入框
     */
    private void updateTranIdVisibility() {
        String direction = (String) ui.directionComboBox.getSelectedItem();
        boolean showTranId = !"無".equals(direction);
        ui.tranIdPanel.setVisible(showTranId);

        if (!showTranId) {
            ui.tranIdField.setText("");
            updateAllClassNames(); // 當隱藏時也更新類名
        } else if (ui.tranIdField.getText().isEmpty() && ui.msgIdField.getText() != null) {
            String msgId = ui.msgIdField.getText().trim();
            String tranId = extractTranId(msgId);
            ui.tranIdField.setText(tranId);
            updateAllClassNames(); // 當設置 tranId 時更新類名
        }

        ui.tranIdPanel.revalidate();
        ui.tranIdPanel.repaint();
    }

    /**
     * 從MSGID中提取電文ID
     * 例如：
     * "B2E-ADHNQ001 哈哈哈" -> "ADHNQ001"
     * "ADHNQ001 測試" -> "ADHNQ001"
     * "XXX-YYY 說明" -> "YYY"
     *
     * @param msgId 原始的MSGID
     * @return 提取的電文ID
     */
    private String extractTranId(String msgId) {
        if (msgId == null || msgId.isEmpty()) {
            return "";
        }

        // 如果包含連字符，取連字符後到空格前的部分
        if (msgId.contains("-")) {
            int startIndex = msgId.lastIndexOf("-") + 1;
            int endIndex = msgId.indexOf(" ", startIndex);
            if (endIndex == -1) {
                endIndex = msgId.length();
            }
            return msgId.substring(startIndex, endIndex);
        }

        // 如果不包含連字符，取開頭到第個空格的���分
        int spaceIndex = msgId.indexOf(" ");
        if (spaceIndex != -1) {
            return msgId.substring(0, spaceIndex);
        }

        // 如果沒有空格，返回整個字符串
        return msgId;
    }

    /**
     * 取有效的ID
     *
     * @return 根據當前電文方向返回應的ID
     */
    private String getEffectiveId() {
        String direction = (String) ui.directionComboBox.getSelectedItem();
        return "無".equals(direction) ? ui.msgIdField.getText().trim() : ui.tranIdField.getText().trim();
    }

    public String getMessageDirection() {
        return (String) ui.directionComboBox.getSelectedItem();
    }

    public String getMessageDirectionComment() {
        String direction = getMessageDirection();
        return switch (direction) {
            case "上行" -> "上行/請求電文";
            case "下行" -> "下行/回應電文";
            default -> "";
        };
    }

    @Override
    protected ValidationInfo doValidate() {
        // 使用驗證器進行驗證
        DtoConfigData configData = new DtoConfigData(
                getTargetPackage(),
                getMsgId(),
                getAuthor(),
                isJava17(),
                getMessageDirection(),
                getCurrentClassNames());
        return DtoConfigValidator.validate(configData, ui.mainClassField);
    }

    @Override
    protected void doOKAction() {
        if (ui.rememberAuthorBox.isSelected()) {
            PropertiesComponent.getInstance().setValue(REMEMBERED_AUTHOR_KEY, getAuthor());
        } else {
            PropertiesComponent.getInstance().unsetValue(REMEMBERED_AUTHOR_KEY);
        }
        super.doOKAction();
    }

    /**
     * 獲取當前所有類名的映射
     *
     * @return 類名映射表，key為類型名，value為對應的類名
     */
    private Map<String, String> getCurrentClassNames() {
        Map<String, String> names = new HashMap<>();
        names.put("main", getMainClassName());
        for (Map.Entry<String, JBTextField> entry : classNameFields.entrySet()) {
            names.put(entry.getKey(), entry.getValue().getText().trim());
        }
        return names;
    }

    public Map<Integer, Map<String, String>> getLevelClassNamesMap() {
        Map<Integer, Map<String, String>> result = new HashMap<>();
        for (Map.Entry<Integer, List<String>> entry : levelTypesMap.entrySet()) {
            Map<String, String> classNames = new HashMap<>();
            for (String typeName : entry.getValue()) {
                String className = getClassName(typeName);
                if (!className.isEmpty()) {
                    classNames.put(typeName, className);
                }
            }
            if (!classNames.isEmpty()) {
                result.put(entry.getKey(), classNames);
            }
        }
        return result;
    }

    /**
     * UI組件類
     * 集中管理所有UI組件��提高代碼的組織性和可維護性
     */
    private class UIComponents {
        final TextFieldWithBrowseButton packageChooser;
        final JBTextField msgIdField;
        final JComboBox<String> directionComboBox;
        final JPanel tranIdPanel;
        final JBTextField tranIdField;
        final JBTextField authorField;
        final JCheckBox rememberAuthorBox;
        final JComboBox<String> javaVersionBox;
        final JBTextField mainClassField;
        final JComboBox<String> jsonPropertyStyleCombo;
        final JList<String> jsonAliasStyleList;
        final JScrollPane jsonAliasScrollPane;
        final JPanel jsonAliasPanel;

        UIComponents() {
            packageChooser = createPackageChooser();
            msgIdField = createTextField(config.msgId);
            directionComboBox = createDirectionComboBox();
            tranIdPanel = new JPanel(new GridBagLayout());
            tranIdField = createTextField("");
            authorField = createTextField(config.author);
            rememberAuthorBox = new JCheckBox("記住作者", !config.author.isEmpty());
            javaVersionBox = createJavaVersionBox();
            mainClassField = createTextField(config.mainClassName);
            jsonPropertyStyleCombo = new JComboBox<>(JSON_STYLES);
            jsonPropertyStyleCombo.addActionListener(e -> updateJsonAliasList());

            jsonAliasStyleList = new JList<>(JSON_ALIAS_OPTIONS);
            jsonAliasStyleList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            jsonAliasStyleList.setVisibleRowCount(4);
            jsonAliasStyleList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            jsonAliasScrollPane = new JBScrollPane(jsonAliasStyleList);
            jsonAliasScrollPane.setPreferredSize(new Dimension(400, 100));

            jsonAliasPanel = new JPanel(new BorderLayout());
            JLabel tipLabel = new JLabel("<html><font color='gray'>提示: 按住 Ctrl 可以選擇多個格式</font></html>");
            tipLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));
            jsonAliasPanel.add(tipLabel, BorderLayout.NORTH);
            jsonAliasPanel.add(jsonAliasScrollPane, BorderLayout.CENTER);

            jsonPropertyStyleCombo.setSelectedItem("原始格式");
            updateJsonAliasList();
        }

        private TextFieldWithBrowseButton createPackageChooser() {
            TextFieldWithBrowseButton chooser = new TextFieldWithBrowseButton();
            chooser.setText(config.initialPackage);
            chooser.addActionListener(e -> showPackageChooserDialog(chooser));
            chooser.getTextField().setEditable(true);
            return chooser;
        }

        private JComboBox<String> createDirectionComboBox() {
            JComboBox<String> box = new JComboBox<>(MESSAGE_DIRECTIONS);
            box.setSelectedItem("無");

            // 設置下拉選單的大小
            box.setPreferredSize(new Dimension(150, FIELD_HEIGHT));
            box.setMaximumSize(new Dimension(150, FIELD_HEIGHT));

            // 使用 ActionListener
            box.addActionListener(e -> {
                updateTranIdVisibility();
                updateAllClassNames();
            });

            return box;
        }

        private JComboBox<String> createJavaVersionBox() {
            JComboBox<String> box = new JComboBox<>(JAVA_VERSIONS);
            box.setSelectedItem(config.isJava17 ? "Java 17" : "Java 8");
            return box;
        }

        private void updateJsonAliasList() {
            String selectedProperty = ((String) jsonPropertyStyleCombo.getSelectedItem()).split(" ")[0];

            DefaultListModel<String> model = new DefaultListModel<>();
            for (String option : JSON_ALIAS_OPTIONS) {
                String format = option.split(" ")[0];
                if (!format.equals(selectedProperty)) {
                    model.addElement(option);
                }
            }

            jsonAliasStyleList.setModel(model);
            jsonAliasStyleList.clearSelection();
        }
    }

    public boolean isRememberAuthor() {
        return ui.rememberAuthorBox.isSelected();
    }

    public String getJsonPropertyStyle() {
        return (String) ui.jsonPropertyStyleCombo.getSelectedItem();
    }

    public List<String> getJsonAliasStyles() {
        return ui.jsonAliasStyleList.getSelectedValuesList().stream()
                .map(style -> style.split(" ")[0])
                .filter(style -> !style.equals("��"))
                .collect(Collectors.toList());
    }
}