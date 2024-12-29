package com.catchaybk.dtogeneratorplugin.intellij.ui.dialog;

import com.catchaybk.dtogeneratorplugin.core.model.DtoField;
import com.catchaybk.dtogeneratorplugin.core.model.UserConfig;
import com.catchaybk.dtogeneratorplugin.intellij.ui.model.DtoTableModel;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

public class DtoGeneratorDialog extends DialogWrapper {
    private static final String REMEMBERED_AUTHOR_KEY = "dto.generator.remembered.author";

    private final DtoTableModel tableModel;
    private final JBTable table;
    private final Project project;
    private DtoConfigDialog configDialog;
    private boolean configurationDone = false;

    // Configuration state
    private Map<Integer, Map<String, String>> levelClassNamesMap = new HashMap<>();
    private String author;
    private String mainClassName = "MainDTO";
    private boolean isJava17;
    private String msgId;
    private boolean isUpstream = true;

    public DtoGeneratorDialog(Project project) {
        super(true);
        this.project = project;
        this.isJava17 = false;
        this.tableModel = new DtoTableModel(isJava17);
        this.table = createTable();

        init();
        setTitle("DTO Generator");
        loadRememberedAuthor();
    }

    private JBTable createTable() {
        JBTable table = new JBTable(tableModel);
        table.getColumnModel().getColumn(2).setCellRenderer(new DtoTableModel.ValidationCellRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new DtoTableModel.ValidationCellRenderer());
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDragEnabled(false);
        table.getTableHeader().setReorderingAllowed(true);
        registerPasteAction(table);
        return table;
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.add(createTablePanel(), BorderLayout.CENTER);
        dialogPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        dialogPanel.setPreferredSize(new Dimension(800, 400));
        return dialogPanel;
    }

    private JComponent createTablePanel() {
        return new JBScrollPane(table);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        addButtons(buttonPanel);
        return buttonPanel;
    }

    private void addButtons(JPanel panel) {
        createAndAddButton(panel, "Setting", e -> showSettingDialog());
        createAndAddButton(panel, "Add Row", e -> tableModel.addEmptyRow());
        createAndAddButton(panel, "Remove Row", e -> removeSelectedRows());
        createAndAddButton(panel, "Paste", e -> handlePaste());
        createAndAddButton(panel, "Configure", e -> showConfigDialog());
    }

    private void createAndAddButton(JPanel panel, String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        panel.add(button);
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
            Messages.showErrorDialog(project, "粘貼數據時發生錯誤: " + ex.getMessage(), "錯誤");
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
        }
    }

    private DtoConfigDialog createConfigDialog() {
        return new DtoConfigDialog(
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
        Map<Integer, List<DtoField>> fieldsByLevel = groupFieldsByLevel(tableModel.getDtoFields());

        for (Map.Entry<Integer, List<DtoField>> entry : fieldsByLevel.entrySet()) {
            Map<String, String> levelMap = new HashMap<>();
            for (DtoField field : entry.getValue()) {
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

    private Map<Integer, List<DtoField>> groupFieldsByLevel(List<DtoField> fields) {
        Map<Integer, List<DtoField>> result = new HashMap<>();
        for (DtoField field : fields) {
            result.computeIfAbsent(field.getLevel(), k -> new ArrayList<>()).add(field);
        }
        return result;
    }

    private Map<Integer, List<String>> collectLevelTypes() {
        Map<Integer, List<String>> levelTypesMap = new TreeMap<>();
        List<DtoField> fields = tableModel.getDtoFields();

        for (DtoField field : fields) {
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

                // ��查是否存在 dto 子目錄
                VirtualFile dtoDir = baseDir.findChild("dto");
                if (dtoDir != null && dtoDir.isDirectory()) {
                    // 如果存在 dto 目錄，返回完整的 dto 包路徑
                    return basePackage + ".dto";
                } else {
                    // 如果不存在 dto 目���，返回當前包路徑
                    return basePackage;
                }
            }
        } catch (Exception e) {
            // 如���發生錯誤，返回空字符串或默認包名
            return "";
        }
        return "";
    }

    private void showConfigurationReminder() {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("DTO Generator Notifications")
                .createNotification(
                        "DTO數據已導入",
                        "請點擊 'Configure' 按鈕設置類名和作者信息",
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
    public List<DtoField> getDtoFields() {
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
