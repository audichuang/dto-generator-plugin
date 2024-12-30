package com.catchaybk.dtogeneratorplugin.intellij.ui.dialog;

import com.catchaybk.dtogeneratorplugin.core.generator.ClassNameGenerator;
import com.catchaybk.dtogeneratorplugin.intellij.ui.factory.ConfigPanelFactory;
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
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
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
public class ConfigDialog extends DialogWrapper {
    // 常量定義
    private static final String TITLE = "DTO Generator Configuration";
    private static final String REMEMBERED_AUTHOR_KEY = "dto.generator.remembered.author";
    private static final String[] MESSAGE_DIRECTIONS = {"無", "上行", "下行"};
    private static final String[] JAVA_VERSIONS = {"Java 8", "Java 17"};
    private static final int LABEL_WIDTH = 150;
    private static final int FIELD_HEIGHT = 30;
    private static final int SCROLL_WIDTH = 800;
    private static final int SCROLL_HEIGHT = 600;
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
    // UI組件
    private final UIComponents ui;
    // 配置數據
    private final ConfigData config;
    private final Project project;
    private final Map<String, JBTextField> classNameFields = new HashMap<>();
    private final Map<Integer, List<String>> levelTypesMap;

    /**
     * 創建配置對話框
     *
     * @param msgId          MSGID
     * @param author         作者
     * @param mainClassName  主類名
     * @param isJava17       是否使用 Java 17
     * @param isUpstream     是否為上行電文
     * @param levelTypesMap  層級類型映射
     * @param project        當前項目
     * @param initialPackage 初始包路徑
     */
    public ConfigDialog(String msgId, String author, String mainClassName,
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

    /**
     * 創建對話框的中心面板
     *
     * @return 配置好的面板組件
     */
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
        JPanel basicPanel = ConfigPanelFactory.createBasicConfigPanel(components, labels);

        // 初始化電文ID面板
        addFormRow(ui.tranIdPanel, "電文ID:", ui.tranIdField, createDefaultConstraints(), 0);
        ui.tranIdPanel.setVisible(false);

        // 創建類型配置面板
        JPanel configPanel = ConfigPanelFactory.createTypeConfigPanel(levelTypesMap, classNameFields);

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
     * 更新電文ID面板的可見性
     * 根據電文方向決定是否顯示電文ID輸入框，並自動填充ID
     */
    private void updateTranIdVisibility() {
        String direction = (String) ui.directionComboBox.getSelectedItem();
        boolean showTranId = !"無".equals(direction);
        ui.tranIdPanel.setVisible(showTranId);

        if (!showTranId) {
            ui.tranIdField.setText("");
            updateAllClassNames();
        } else if (ui.tranIdField.getText().isEmpty() && ui.msgIdField.getText() != null) {
            String msgId = ui.msgIdField.getText().trim();
            String tranId = extractTranId(msgId);
            ui.tranIdField.setText(tranId);
            updateAllClassNames();
        }

        ui.tranIdPanel.revalidate();
        ui.tranIdPanel.repaint();
    }

    /**
     * 從MSGID中提取電文ID
     *
     * @param msgId 原始的MSGID
     * @return 提取的電文ID
     */
    private String extractTranId(String msgId) {
        if (StringUtils.isBlank(msgId)) {
            return "";
        }

        // 先用空格分割，取得前半部分的英文代碼
        String[] parts = StringUtils.split(msgId, " ", 2);
        if (parts == null || parts.length == 0) {
            return "";
        }

        String codeSection = parts[0];
        // 用 - 或 _ 分割英文代碼部分
        String[] codeParts = StringUtils.split(codeSection, "[-_]");
        if (codeParts == null || codeParts.length == 0) {
            return "";
        }

        // 取得最後一個部分
        String lastPart = codeParts[codeParts.length - 1];

        return lastPart;
    }

    // Getter 方法
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

    public String getMessageDirection() {
        return (String) ui.directionComboBox.getSelectedItem();
    }

    public boolean isRememberAuthor() {
        return ui.rememberAuthorBox.isSelected();
    }

    public String getJsonPropertyStyle() {
        return (String) ui.jsonPropertyStyleCombo.getSelectedItem();
    }

    /**
     * 獲取選中的 JSON Alias 格式列表
     *
     * @return JSON Alias 格式列表
     */
    public List<String> getJsonAliasStyles() {
        return ui.jsonAliasStyleList.getSelectedValuesList().stream()
                .map(style -> style.split(" ")[0])
                .filter(style -> !style.equals("無"))
                .collect(Collectors.toList());
    }

    /**
     * 獲取電文方向的註釋
     */
    public String getMessageDirectionComment() {
        return switch (getMessageDirection()) {
            case "上行" -> "上行/請求電文";
            case "下行" -> "下行/回應電文";
            default -> "";
        };
    }

    /**
     * 獲取層級類名映射
     */
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
     * 創建文檔變更監聽器
     *
     * @return 文檔監聽器實例
     */
    private DocumentListener createDocumentListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateAllClassNames());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateAllClassNames());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateAllClassNames());
            }
        };
    }

    /**
     * 更新所有類名
     * 當MSGID或電文方向改變時自動更新所有相關類名
     */
    private void updateAllClassNames() {
        String direction = (String) ui.directionComboBox.getSelectedItem();
        String effectiveId = getEffectiveId();

        // 更新主類名
        String mainClassName = ClassNameGenerator.generateClassName(
                effectiveId, direction, "", true);
        ui.mainClassField.setText(mainClassName);

        // 更新所有子類名
        for (Map.Entry<String, JBTextField> entry : classNameFields.entrySet()) {
            String baseName = entry.getKey();
            String newClassName = ClassNameGenerator.generateClassName(
                    effectiveId, direction, baseName, false);
            entry.getValue().setText(newClassName);
        }
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

    @Override
    protected ValidationInfo doValidate() {
        // 只驗證必要的字段
        if (getTargetPackage().trim().isEmpty()) {
            return new ValidationInfo("請選擇目標包路徑", ui.mainClassField);
        }
        if (getMainClassName().trim().isEmpty()) {
            return new ValidationInfo("主類名不能為空", ui.mainClassField);
        }
        if (getAuthor().trim().isEmpty()) {
            return new ValidationInfo("請輸入作者名稱", ui.mainClassField);
        }
        return null;
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

    public String getClassName(String typeName) {
        JBTextField field = classNameFields.get(typeName);
        return field != null ? field.getText().trim() : "";
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

    /**
     * UI組件類
     * 集中管理所有UI組件，提高代碼的組織性和可維護性
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
}