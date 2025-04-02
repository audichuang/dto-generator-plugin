package com.catchaybk.dtogeneratorplugin.intellij.ui.dialog;

import com.catchaybk.dtogeneratorplugin.core.model.Field;
import com.catchaybk.dtogeneratorplugin.core.model.UserConfig;
import com.catchaybk.dtogeneratorplugin.intellij.ui.model.FieldTableModel;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

/**
 * 生成器對話框
 * 用於收集和驗證DTO生成所需的數據
 */
public class GeneratorDialog extends DialogWrapper {
    private static final String REMEMBERED_AUTHOR_KEY = "dto.generator.remembered.author";
    private static final Color HEADER_COLOR = new JBColor(new Color(240, 240, 240), new Color(50, 50, 50));
    private static final Color BUTTON_BACKGROUND = new JBColor(new Color(0, 122, 204), new Color(64, 81, 153));
    private static final Color HELP_BACKGROUND = new JBColor(new Color(250, 250, 220), new Color(60, 63, 50));
    private static final Color BUTTON_FOREGROUND = new JBColor(Color.WHITE, Color.WHITE);
    private static final int TABLE_ROW_HEIGHT = 28;

    private final FieldTableModel tableModel;
    private final JBTable table;
    private final Project project;
    private ConfigDialog configDialog;
    private boolean configurationDone = false;

    // 配置狀態
    private Map<Integer, Map<String, String>> levelClassNamesMap = new HashMap<>();
    private String author;
    private String mainClassName = "Main";
    private boolean isJava17;
    private String msgId;
    private boolean isUpstream = true;

    public GeneratorDialog(Project project) {
        super(true);
        this.project = project;
        this.isJava17 = false;
        this.tableModel = new FieldTableModel(isJava17);
        this.table = createTable();

        init();
        setTitle("DTO 生成器");
        loadRememberedAuthor();
    }

    private JBTable createTable() {
        JBTable table = new JBTable(tableModel);

        // 設置表格外觀
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setShowGrid(true);
        table.setGridColor(JBColor.border().darker());
        table.setStriped(true);

        // 自定義表頭
        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int column) {
                JComponent comp = (JComponent) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                comp.setBorder(new MatteBorder(0, 0, 1, 1, JBColor.border()));
                comp.setBackground(UIUtil.getPanelBackground().darker());
                comp.setForeground(JBColor.foreground());

                // 設置列標題提示
                String toolTip = switch (column) {
                    case 0 -> "欄位層級 (1-3)";
                    case 1 -> "欄位名稱 (例如: userId)";
                    case 2 -> "數據類型 (例如: String, Integer, List<User>)";
                    case 3 -> "數據長度限制 (例如: 字串使用'50'，小數使用'10,2')";
                    case 4 -> "是否必填 (Y/N)";
                    case 5 -> "欄位說明";
                    case 6 -> "輸入驗證正則表達式";
                    default -> null;
                };
                ((JComponent) comp).setToolTipText(toolTip);

                return comp;
            }
        });

        table.getColumnModel().getColumn(2).setCellRenderer(new FieldTableModel.ValidationCellRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new FieldTableModel.ValidationCellRenderer());
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDragEnabled(false);

        // 設置表格提示文字
        table.setToolTipText("填寫DTO欄位信息，可以直接從Excel貼上數據");

        // 添加列移動監聽器
        table.getTableHeader().setReorderingAllowed(true);
        table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            @Override
            public void columnMoved(TableColumnModelEvent e) {
                if (e.getFromIndex() != e.getToIndex()) {
                    tableModel.updateColumnOrder(table.getColumnModel());
                }
            }

            @Override
            public void columnAdded(TableColumnModelEvent e) {
            }

            @Override
            public void columnRemoved(TableColumnModelEvent e) {
            }

            @Override
            public void columnMarginChanged(ChangeEvent e) {
            }

            @Override
            public void columnSelectionChanged(ListSelectionEvent e) {
            }
        });

        registerPasteAction(table);
        return table;
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        // 創建頂部面板
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(HEADER_COLOR);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, JBColor.border()),
                JBUI.Borders.empty(12, 15)));

        JLabel titleLabel = new JBLabel("DTO 欄位定義");
        titleLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD, 16f));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        dialogPanel.add(headerPanel, BorderLayout.NORTH);

        // 添加操作提示面板
        JPanel instructionPanel = new JBPanel<>();
        instructionPanel.setLayout(new BoxLayout(instructionPanel, BoxLayout.Y_AXIS));
        instructionPanel.setBackground(HELP_BACKGROUND);
        instructionPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, JBColor.border()),
                JBUI.Borders.empty(8, 15)));

        JLabel instructionLabel = new JBLabel("<html><b>使用說明:</b><ul style='margin-top:0;'>" +
                "<li>從Excel複製欄位數據並直接貼上</li>" +
                "<li>添加完數據後點擊 <b>配置</b> 按鈕設置類名和其他信息</li>" +
                "<li>所有欄位必須指定資料類型</li>" +
                "<li>物件類型將自動生成子DTO類</li>" +
                "</ul></html>");
        instructionPanel.add(instructionLabel);

        dialogPanel.add(instructionPanel, BorderLayout.NORTH);

        // 創建內容面板
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(createTablePanel(), BorderLayout.CENTER);
        contentPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        dialogPanel.add(contentPanel, BorderLayout.CENTER);
        dialogPanel.setPreferredSize(new Dimension(900, 600));
        return dialogPanel;
    }

    private JComponent createTablePanel() {
        JBScrollPane scrollPane = new JBScrollPane(table);
        scrollPane.setBorder(JBUI.Borders.empty(10));
        return scrollPane;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, JBColor.border()),
                JBUI.Borders.empty(10, 15)));

        // 創建左側輔助按鈕面板
        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        addHelperButtons(leftButtonPanel);
        buttonPanel.add(leftButtonPanel, BorderLayout.WEST);

        // 創建右側主要按鈕面板
        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        addMainButtons(rightButtonPanel);
        buttonPanel.add(rightButtonPanel, BorderLayout.EAST);

        return buttonPanel;
    }

    private void addHelperButtons(JPanel panel) {
        JButton settingButton = createStyledButton("系統設定", e -> showSettingDialog());
        settingButton.setToolTipText("打開驗證消息設定對話框");
        panel.add(settingButton);

        JButton pasteButton = createStyledButton("貼上", e -> handlePaste());
        pasteButton.setToolTipText("從剪貼板貼上表格數據");
        panel.add(pasteButton);
    }

    private void addMainButtons(JPanel panel) {
        JButton addButton = createStyledButton("添加行", e -> tableModel.addEmptyRow());
        addButton.setToolTipText("添加一個新的空行");
        panel.add(addButton);

        JButton removeButton = createStyledButton("刪除行", e -> removeSelectedRows());
        removeButton.setToolTipText("刪除選中的行");
        panel.add(removeButton);

        JButton configButton = createStyledButton("配置", e -> showConfigDialog());
        configButton.setBackground(BUTTON_BACKGROUND);
        configButton.setForeground(BUTTON_FOREGROUND);
        configButton.setFont(configButton.getFont().deriveFont(Font.BOLD));
        configButton.setToolTipText("配置DTO類名和其他信息");
        panel.add(configButton);
    }

    private JButton createStyledButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(100, 35));
        button.setMargin(JBUI.insets(5, 15));
        button.setFocusPainted(false);
        button.addActionListener(listener);
        return button;
    }

    private void registerPasteAction(JBTable table) {
        KeyStroke paste = KeyStroke.getKeyStroke("control V");
        table.registerKeyboardAction(
                e -> handlePaste(),
                "Paste",
                paste,
                JComponent.WHEN_FOCUSED);
    }

    private void handlePaste() {
        try {
            int oldRowCount = tableModel.getRowCount();
            String clipboardData = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);
            tableModel.processClipboardData(clipboardData);

            // 只有在行數增加時才顯示提示
            if (tableModel.getRowCount() > oldRowCount) {
                showConfigurationReminder();
            }
        } catch (Exception ex) {
            Messages.showErrorDialog(project, "貼上數據時發生錯誤: " + ex.getMessage(), "錯誤");
        }
    }

    private void removeSelectedRows() {
        int[] selectedRows = table.getSelectedRows();
        Arrays.sort(selectedRows);
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            tableModel.removeRow(selectedRows[i]);
        }
    }

    private void showConfigDialog() {
        if (tableModel.getRowCount() == 0) {
            Messages.showWarningDialog(project, "請先添加數據再進行配置", "警告");
            return;
        }

        // 檢查數據類型和Size格式
        Set<String> unknownTypes = tableModel.validateTypes();
        if (unknownTypes == null) { // null 表示有錯誤（空值或Size格式錯誤）
            return;
        }

        // 處理未知類型的警告
        if (!unknownTypes.isEmpty()) {
            String unknownTypesStr = String.join(", ", unknownTypes);
            String message = String.format(
                    "發現未知的數據類型：\n%s\n\n" +
                            "這些類型系統沒有見過，可能是：\n" +
                            "1. 新的自定義類型\n" +
                            "2. 輸入錯誤\n\n" +
                            "請確認是否要繼續？",
                    unknownTypesStr);

            String[] options = { "繼續", "取消" };
            int result = Messages.showDialog(
                    message,
                    "警告",
                    options,
                    0,
                    Messages.getWarningIcon());

            if (result != 0) { // 不繼續
                return;
            }
        }

        configDialog = createConfigDialog();
        if (configDialog.showAndGet()) {
            updateConfigurationFromDialog();
            configurationDone = true;

            // 顯示配置完成提示
            showCompletionNotification();
        }
    }

    private void showCompletionNotification() {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("DTO Generator Notifications")
                .createNotification(
                        "DTO配置完成",
                        "DTO配置已完成，點擊確定按鈕生成代碼",
                        NotificationType.INFORMATION)
                .notify(project);
    }

    private ConfigDialog createConfigDialog() {
        return new ConfigDialog(
                msgId,
                author,
                mainClassName,
                isJava17,
                isUpstream,
                collectLevelTypes(),
                project,
                getCurrentPackage());
    }

    private void updateConfigurationFromDialog() {
        if (configDialog == null) {
            return;
        }

        // 更新基本配置
        msgId = configDialog.getMsgId();
        author = configDialog.getAuthor();
        mainClassName = configDialog.getMainClassName();
        isJava17 = configDialog.isJava17();

        // 更新 TableModel 的 Java 版本設置
        tableModel.updateJavaVersion(isJava17);

        // 更新類名映射
        levelClassNamesMap.clear();
        Map<Integer, List<Field>> fieldsByLevel = groupFieldsByLevel(tableModel.getDtoFields());

        for (Map.Entry<Integer, List<Field>> entry : fieldsByLevel.entrySet()) {
            Map<String, String> levelMap = new HashMap<>();
            for (Field field : entry.getValue()) {
                if (field.isObject()) {
                    // 使用配置對話框中的類名
                    String className = configDialog.getClassName(field.getCapitalizedName());
                    if (!className.isEmpty()) {
                        levelMap.put(field.getDataName(), className);
                    }
                }
            }
            if (!levelMap.isEmpty()) {
                levelClassNamesMap.put(entry.getKey(), levelMap);
            }
        }

        // 保存作者信息（如果需要）
        if (configDialog.isRememberAuthor()) {
            PropertiesComponent.getInstance().setValue(REMEMBERED_AUTHOR_KEY, author);
        }
    }

    private Map<Integer, List<Field>> groupFieldsByLevel(List<Field> fields) {
        Map<Integer, List<Field>> result = new HashMap<>();
        for (Field field : fields) {
            result.computeIfAbsent(field.getLevel(), k -> new ArrayList<>()).add(field);
        }
        return result;
    }

    private Map<Integer, List<String>> collectLevelTypes() {
        Map<Integer, List<String>> levelTypesMap = new TreeMap<>();
        List<Field> fields = tableModel.getDtoFields();

        for (Field field : fields) {
            if (field.isObject()) {
                levelTypesMap
                        .computeIfAbsent(field.getLevel(), k -> new ArrayList<>())
                        .add(field.getCapitalizedName());
            }
        }
        return levelTypesMap;
    }

    private String getCurrentPackage() {
        try {
            VirtualFile currentFile = FileEditorManager.getInstance(project).getSelectedFiles()[0];
            PsiFile psiFile = PsiManager.getInstance(project).findFile(currentFile);

            if (psiFile instanceof PsiJavaFile) {
                PsiJavaFile javaFile = (PsiJavaFile) psiFile;
                String basePackage = javaFile.getPackageName();
                VirtualFile baseDir = currentFile.getParent();

                // 檢查是否存在 dto 子目錄
                VirtualFile dtoDir = baseDir.findChild("dto");
                if (dtoDir != null && dtoDir.isDirectory()) {
                    // 如果存在 dto 目錄，返回完整的 dto 包路徑
                    return basePackage + ".dto";
                } else {
                    // 如果不存在 dto 目錄，返回當前包路徑
                    return basePackage;
                }
            }
        } catch (Exception e) {
            // 如果發生錯誤，返回空字符串或默認包名
            return "";
        }
        return "";
    }

    private void showConfigurationReminder() {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("DTO Generator Notifications")
                .createNotification(
                        "DTO數據已導入",
                        "請點擊 '配置' 按鈕設置類名和作者信息",
                        NotificationType.INFORMATION)
                .notify(project);
    }

    private void loadRememberedAuthor() {
        author = PropertiesComponent.getInstance().getValue(REMEMBERED_AUTHOR_KEY, "");
    }

    @Override
    protected void doOKAction() {
        if (!configurationDone) {
            Messages.showWarningDialog(project, "請先完成配置", "警告");
            return;
        }

        if (!tableModel.validateDataTypes()) {
            Messages.showErrorDialog(project, "請填寫所有欄位的數據類型", "錯誤");
            return;
        }

        super.doOKAction();
    }

    // Getters
    public List<Field> getDtoFields() {
        return tableModel.getDtoFields();
    }

    public String getAuthor() {
        return author;
    }

    public String getMainClassName() {
        return mainClassName;
    }

    public boolean isJava17() {
        return isJava17;
    }

    public String getMsgId() {
        return msgId;
    }

    public Map<Integer, Map<String, String>> getLevelClassNamesMap() {
        return new HashMap<>(levelClassNamesMap);
    }

    public String getMessageDirectionComment() {
        return configDialog != null ? configDialog.getMessageDirectionComment() : "";
    }

    public String getTargetPackage() {
        return configDialog != null ? configDialog.getTargetPackage() : "dto";
    }

    private void showSettingDialog() {
        ValidationMessageSettingDialog dialog = new ValidationMessageSettingDialog();
        dialog.show();
    }

    public UserConfig getUserConfig() {
        return new UserConfig(
                getDtoFields(),
                getMainClassName(),
                getAuthor(),
                getMsgId(),
                isJava17(),
                getMessageDirectionComment(),
                getLevelClassNamesMap(),
                getTargetPackage(),
                configDialog != null ? configDialog.getJsonPropertyStyle().split(" ")[0] : "原始格式",
                configDialog != null ? configDialog.getJsonAliasStyles() : Collections.emptyList());
    }
}