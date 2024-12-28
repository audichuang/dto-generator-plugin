package com.catchaybk.dtogeneratorplugin.ui;

import com.catchaybk.dtogeneratorplugin.model.DtoField;
import com.catchaybk.dtogeneratorplugin.model.DtoStructure;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.util.List;
import java.util.*;

public class DtoGeneratorDialog extends DialogWrapper {
    private JBTable table;
    private DefaultTableModel tableModel;
    private Map<Integer, Map<String, String>> levelClassNamesMap = new HashMap<>();
    private String author;
    private boolean configurationDone = false;
    private JCheckBox rememberAuthorCheckBox;
    private static final String REMEMBERED_AUTHOR_KEY = "dto.generator.remembered.author";
    private String mainClassName = "MainDTO"; // 添加字段
    private boolean isJava17;
    private String msgId;
    private boolean isUpstream = true;
    private DtoConfigDialog configDialog;

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public boolean isJava17() {
        return isJava17;
    }

    public void setJava17(boolean java17) {
        isJava17 = java17;
    }

    public DtoGeneratorDialog() {
        super(true);
        initializeTable();
        init();
        setTitle("DTO Generator");
        loadRememberedAuthor();
    }

    private void initializeTable() {
        String[] columnNames = {"Level", "Data Name", "Data Type", "Size", "Required", "Comments"};
        tableModel = new DefaultTableModel(columnNames, 0);
        table = new JBTable(tableModel);
        // 設置自定義的單元格渲染器
        table.getColumnModel().getColumn(2).setCellRenderer(new ValidationCellRenderer());

    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        // 添加表格和滾動面板
        JBScrollPane scrollPane = new JBScrollPane(table);
        dialogPanel.add(scrollPane, BorderLayout.CENTER);

        // 添加按鈕面板
        JPanel buttonPanel = new JPanel();
        JButton addRowButton = new JButton("Add Row");
        JButton removeRowButton = new JButton("Remove Row");
        JButton pasteButton = new JButton("Paste");
        JButton configButton = new JButton("Configure");

        addRowButton.addActionListener(e -> addRow());
        removeRowButton.addActionListener(e -> removeSelectedRows());
        pasteButton.addActionListener(e -> handlePaste());
        configButton.addActionListener(e -> showConfigDialog());

        buttonPanel.add(addRowButton);
        buttonPanel.add(removeRowButton);
        buttonPanel.add(pasteButton);
        buttonPanel.add(configButton);
        dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 註冊快捷鍵
        registerPasteAction();

        dialogPanel.setPreferredSize(new Dimension(800, 400));
        return dialogPanel;
    }

    private void registerPasteAction() {
        KeyStroke paste = KeyStroke.getKeyStroke("control V");
        table.registerKeyboardAction(
                e -> handlePaste(),
                "Paste",
                paste,
                JComponent.WHEN_FOCUSED
        );
    }

    private void loadRememberedAuthor() {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        author = propertiesComponent.getValue(REMEMBERED_AUTHOR_KEY, "");
    }

    private void handlePaste() {
        try {
            String clipboardData = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);

            // 首先按行分割
            String[] rows = clipboardData.split("\n");
            for (String row : rows) {
                processRow(row.trim());
            }
            showConfigurationReminder();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void processRow(String row) {
        if (row.isEmpty()) return;

        String[] columns = new String[6];
        Arrays.fill(columns, ""); // 初始化為空字符串

        // 檢查是否是狀態說明行（以數字加冒號開始）
        if (row.matches("^\\d+:\\s+.*")) {
            // 將狀態說明添加到前一行的註釋中
            int lastRow = tableModel.getRowCount() - 1;
            if (lastRow >= 0) {
                String currentComment = (String) tableModel.getValueAt(lastRow, 5);
                String newComment = currentComment + "\n" + row.trim();
                tableModel.setValueAt(newComment, lastRow, 5);
            }
            return;
        }

        // 使用固定位置的方式處理
        String[] parts = row.split("\\t|(?:  +)");
        int currentColumn = 0;
        StringBuilder comment = new StringBuilder();
        boolean isComment = false;

        for (String part : parts) {
            if (part.trim().isEmpty()) continue;

            if (isComment || currentColumn > 4) {
                // 已經到註釋部分
                if (comment.length() > 0) comment.append(" ");
                comment.append(part.trim());
                isComment = true;
            } else {
                // 特殊處理第3列（Size）
                if (currentColumn == 3) {
                    String trimmedPart = part.trim();
                    // 檢查是否是有效的 Size 值（數字）
                    if (trimmedPart.matches("\\d+")) {
                        columns[currentColumn] = trimmedPart;
                        currentColumn++;
                    } else {
                        // 如果不是有效的 Size 值，則視為註釋的開始
                        if (comment.length() > 0) comment.append(" ");
                        comment.append(part.trim());
                        isComment = true;
                    }
                }
                // 特殊處理第4列（Required）
                else if (currentColumn == 4) {
                    String trimmedPart = part.trim();
                    // 檢查是否是有效的 Required 值（Y、N、-） 值（Y、N、-）
                    if (trimmedPart.matches("[YN-]")) {
                        columns[currentColumn] = trimmedPart;
                        currentColumn++;
                    } else {
                        // 如果不是有效的 Required 值，則視為註釋的開始
                        if (comment.length() > 0) comment.append(" ");
                        comment.append(part.trim());
                        isComment = true;
                    }
                } else {
                    columns[currentColumn] = part.trim();
                    currentColumn++;
                }
            }
        }

        // 設置註釋
        if (comment.length() > 0) {
            columns[5] = comment.toString().trim();
        }

        // 只有在必要欄位存在時才添加行
        if (!columns[0].isEmpty() && !columns[1].isEmpty()) {
            tableModel.addRow(columns);
        }
    }


    private void addTableRow(String[] cells) {
        try {
            String level = cells[0];
            String dataName = cells[1];
            String dataType = cells[2];
            String size = cells[3];
            String required = cells[4];
            String comments = cells.length > 5 ? cells[5].trim() : "";

            // 驗證數據
            if (level.matches("\\d+") && !dataName.isEmpty()) {
                // 對於 List<Object> 這樣的類型，保持原樣
                tableModel.addRow(new Object[]{
                        level,
                        dataName,
                        dataType,
                        size,
                        required,
                        comments
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showConfigurationReminder() {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("DTO Generator Notifications")
                .createNotification(
                        "DTO數據已導入",
                        "請點擊 'Configure' 按鈕設置類名和作者信息",
                        NotificationType.INFORMATION
                )
                .notify(null);
    }

    private Map<Integer, List<DtoStructure>> analyzeDtoStructure() {
        Map<Integer, List<DtoStructure>> structures = new HashMap<>();
        List<DtoField> fields = getDtoFields();

        // 主層級（Level 0）的欄位
        List<DtoField> mainFields = new ArrayList<>();
        // SupList 層級（Level 1）的欄位
        List<DtoField> supListFields = new ArrayList<>();
        // SubSeqnoList 層級（Level 2）的欄位
        List<DtoField> subSeqnoListFields = new ArrayList<>();

        boolean inSupList = false;
        boolean inSubSeqnoList = false;
        String currentParentField = null;

        for (DtoField field : fields) {
            if (field.getDataName().equals("SupList") && field.getDataType().contains("List")) {
                // 開始 SupList 區域
                if (!mainFields.isEmpty()) {
                    addStructure(structures, 0, null, new ArrayList<>(mainFields));
                }
                inSupList = true;
                currentParentField = "SupList";
                mainFields.add(field);
                continue;
            }

            if (field.getDataName().equals("SubSeqnoList") && field.getDataType().contains("List")) {
                // 開始 SubSeqnoList 區域
                if (!supListFields.isEmpty()) {
                    addStructure(structures, 1, currentParentField, new ArrayList<>(supListFields));
                    supListFields.clear();
                }
                inSubSeqnoList = true;
                supListFields.add(field);
                continue;
            }

            // 根據當前狀態將欄位添加到相應的集合中
            if (inSubSeqnoList) {
                subSeqnoListFields.add(field);
            } else if (inSupList) {
                supListFields.add(field);
            } else {
                mainFields.add(field);
            }
        }

        // 處理剩餘的欄位
        if (!mainFields.isEmpty()) {
            addStructure(structures, 0, null, mainFields);
        }
        if (!supListFields.isEmpty()) {
            addStructure(structures, 1, "SupList", supListFields);
        }
        if (!subSeqnoListFields.isEmpty()) {
            addStructure(structures, 2, "SubSeqnoList", subSeqnoListFields);
        }

        return structures;
    }

    private void addStructure(Map<Integer, List<DtoStructure>> structures,
                              int level, String parentField, List<DtoField> fields) {
        structures.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new DtoStructure(level, parentField, new ArrayList<>(fields)));
    }

    private void showConfigDialog() {
        if (tableModel.getRowCount() == 0) {
            Messages.showWarningDialog("請先添加數據再進行配置", "警告");
            return;
        }

        if (!validateDataTypes()) {
            return;
        }

        // 按層級收集自定義類型
        Map<Integer, List<String>> levelTypesMap = new TreeMap<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String dataName = tableModel.getValueAt(i, 1).toString();
            String dataType = tableModel.getValueAt(i, 2).toString();
            int level = Integer.parseInt(tableModel.getValueAt(i, 0).toString());

            // 創建一個臨時的 DtoField 來使用其類型判斷邏輯
            DtoField tempField = new DtoField(level, dataName, dataType, "", true, "");

            // 只有當字段是對象類型或是非基本類型的列表時才需要配置
            if (tempField.isObject()) {
                levelTypesMap
                        .computeIfAbsent(level, k -> new ArrayList<>())
                        .add(tempField.getCapitalizedName()); // 使用首字母大寫的版本
            }
        }

        // 創建配置對話框
        configDialog = new DtoConfigDialog(
                msgId,
                author,
                mainClassName,
                isJava17,
                isUpstream,
                levelTypesMap
        );

        if (configDialog.showAndGet()) {
            // 獲取基本配置
            msgId = configDialog.getMsgId();
            author = configDialog.getAuthor();
            mainClassName = configDialog.getMainClassName();
            isJava17 = configDialog.isJava17();

            // 獲取所有自定義類型的類名配置
            levelClassNamesMap.clear();
            for (Map.Entry<Integer, List<String>> entry : levelTypesMap.entrySet()) {
                int level = entry.getKey();
                for (String typeName : entry.getValue()) {
                    String className = configDialog.getClassName(typeName);
                    if (!className.isEmpty()) {
                        // 存儲時使用原始的 dataName 作為鍵
                        String originalDataName = findOriginalDataName(typeName);
                        levelClassNamesMap
                                .computeIfAbsent(level, k -> new HashMap<>())
                                .put(originalDataName, className);
                    }
                }
            }

            configurationDone = true;
        }
    }

    // 添加輔助方法來查找原始的 dataName
    private String findOriginalDataName(String capitalizedName) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String dataName = tableModel.getValueAt(i, 1).toString();
            if (dataName.equalsIgnoreCase(capitalizedName)) {
                return dataName;
            }
        }
        return capitalizedName;
    }

    public String getMessageDirectionComment() {
        return configDialog != null ? configDialog.getMessageDirectionComment() : "";
    }

    private boolean validateDataTypes() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        boolean hasError = false;

        // 遍歷所有行檢查數據類型
        for (int i = 0; i < model.getRowCount(); i++) {
            String dataType = (String) model.getValueAt(i, 2);
            if (dataType == null || dataType.trim().isEmpty()) {
                hasError = true;
            }
        }

        // 強制表格重繪以更新單元格背景色
        table.repaint();

        if (hasError) {
            Messages.showErrorDialog(
                    "請填寫所有欄位的數據類型",
                    "數據類型缺失"
            );
            return false;
        }

        return true;
    }


    // 輔助方法：獲取類型的層級
    private int getTypeLevel(String typeName) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 1).toString().equals(typeName)) {
                return Integer.parseInt(tableModel.getValueAt(i, 0).toString());
            }
        }
        return 0;
    }

    private int calculateMaxLevel() {
        boolean hasSupList = false;
        boolean hasSubSeqnoList = false;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String dataName = tableModel.getValueAt(i, 1).toString();
            String dataType = tableModel.getValueAt(i, 2).toString();

            if (dataName.equals("SupList") && dataType.contains("List")) {
                hasSupList = true;
            } else if (dataName.equals("SubSeqnoList") && dataType.contains("List")) {
                hasSubSeqnoList = true;
            }
        }

        if (hasSubSeqnoList) return 2;
        if (hasSupList) return 1;
        return 0;
    }


    // 輔助方法：判斷是否為原始類型
    private boolean isPrimitiveType(String type) {
        if (type == null) return false;
        Set<String> primitiveTypes = new HashSet<>(Arrays.asList(
                "string", "int", "integer", "long", "double", "float",
                "boolean", "date", "datetime", "bigdecimal", "char",
                "byte", "short", "void", "decimal"
        ));
        return primitiveTypes.contains(type.toLowerCase());
    }

    private void addRow() {
        tableModel.addRow(new Object[]{"", "", "", "", "", ""});
    }

    private void removeSelectedRows() {
        int[] selectedRows = table.getSelectedRows();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            tableModel.removeRow(selectedRows[i]);
        }
    }

    public List<DtoField> getDtoFields() {
        List<DtoField> fields = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                int level = Integer.parseInt(tableModel.getValueAt(i, 0).toString());
                String dataName = tableModel.getValueAt(i, 1).toString();
                String dataType = tableModel.getValueAt(i, 2).toString();
                String size = tableModel.getValueAt(i, 3).toString();
                String requiredStr = tableModel.getValueAt(i, 4).toString().trim(); // 獲取 Required 欄位值
                boolean required = "Y".equalsIgnoreCase(requiredStr); // 只有 Y 才是必填
                String comments = tableModel.getValueAt(i, 5).toString();

                DtoField field = new DtoField(level, dataName, dataType, size, required, comments);
                field.setRequiredString(requiredStr); // 設置原始的 required 字符串
                fields.add(field);
            } catch (Exception e) {
                // 跳過無效的行
                continue;
            }
        }
        return fields;
    }

    @Override
    protected void doOKAction() {
        if (!configurationDone) {
            Messages.showWarningDialog("請先完成配置", "警告");
            return;
        }

        // 在完成操作前再次驗證數據類型
        if (!validateDataTypes()) {
            return;
        }
        super.doOKAction();
    }

    public Map<Integer, Map<String, String>> getLevelClassNamesMap() {
        return levelClassNamesMap;
    }

    public String getAuthor() {
        return author;
    }

    public String getMainClassName() {
        return mainClassName;
    }

    public boolean isUpstream() {
        return isUpstream;
    }
}

// 自定義的 TableCellRenderer
class ValidationCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {
        Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        if (column == 2) { // Data Type 列
            String dataType = value != null ? value.toString() : "";
            if (dataType.trim().isEmpty()) {
                setBackground(new Color(255, 200, 200)); // 淺紅色
            } else {
                setBackground(table.getBackground()); // 恢復默認背景色
            }
        } else {
            setBackground(table.getBackground()); // 其他列使用默認背景色
        }

        return c;
    }
}
