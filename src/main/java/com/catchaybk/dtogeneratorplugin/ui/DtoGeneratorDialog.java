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
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.util.*;
import java.util.List;

public class DtoGeneratorDialog extends DialogWrapper {
    private JBTable table;
    private DefaultTableModel tableModel;
    private Map<Integer, Map<String, String>> levelClassNamesMap = new HashMap<>();
    private String author;
    private boolean configurationDone = false;
    private JCheckBox rememberAuthorCheckBox;
    private static final String REMEMBERED_AUTHOR_KEY = "dto.generator.remembered.author";
    private String mainClassName = "MainDTO"; // 添加字段

    public DtoGeneratorDialog() {
        super(true);
        initializeTable();
        init();
        setTitle("DTO Generator");
        loadRememberedAuthor();
    }

    private void initializeTable() {
        String[] columnNames = {"Level", "Data Name", "Data Type", "Size", "Nullable", "Comments"};
        tableModel = new DefaultTableModel(columnNames, 0);
        table = new JBTable(tableModel);
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

        String[] columns = new String[6]; // Level, DataName, DataType, Size, Nullable, Comments
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
                // 檢查是否是註釋的開始（通常是中文字符或特殊標記）
                if (currentColumn == 4 && (part.matches(".*[\\u4e00-\\u9fa5].*") || !part.matches("[YN]"))) {
                    // 如果第5列不是 Y/N，則視為註釋的開始
                    if (comment.length() > 0) comment.append(" ");
                    comment.append(part.trim());
                    isComment = true;
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
            String nullable = cells[4];
            String comments = cells.length > 5 ? cells[5].trim() : "";

            // 驗證數據
            if (level.matches("\\d+") && !dataName.isEmpty()) {
                // 對於 List<Object> 這樣的類型，保持原樣
                tableModel.addRow(new Object[]{
                        level,
                        dataName,
                        dataType,
                        size,
                        nullable,
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

        int currentLevel = 1;
        String currentParentField = null;
        List<DtoField> currentFields = new ArrayList<>();

        for (DtoField field : fields) {
            if (field.getLevel() == currentLevel) {
                currentFields.add(field);
                if (field.isList()) {
                    currentParentField = field.getDataName();
                }
            } else if (field.getLevel() > currentLevel) {
                if (!currentFields.isEmpty()) {
                    addStructure(structures, currentLevel, currentParentField, currentFields);
                    currentFields = new ArrayList<>();
                }
                currentLevel = field.getLevel();
                currentFields.add(field);
            } else {
                if (!currentFields.isEmpty()) {
                    addStructure(structures, currentLevel, currentParentField, currentFields);
                    currentFields = new ArrayList<>();
                }
                currentLevel = field.getLevel();
                currentParentField = null;
                currentFields.add(field);
            }
        }

        if (!currentFields.isEmpty()) {
            addStructure(structures, currentLevel, currentParentField, currentFields);
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

        // 分析每個層級的對象
        Map<Integer, Set<String>> levelObjects = new HashMap<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                int level = Integer.parseInt(tableModel.getValueAt(i, 0).toString());
                String dataName = tableModel.getValueAt(i, 1).toString();
                String dataType = tableModel.getValueAt(i, 2).toString().toLowerCase();

                // 如果是對象類型或列表類型，添加到對應層級的集合中
                if (dataType.equals("object") || dataType.startsWith("list") ||
                        dataType.contains("list<") || !isPrimitiveType(dataType)) {
                    levelObjects.computeIfAbsent(level, k -> new HashSet<>()).add(dataName);
                }
            } catch (NumberFormatException e) {
                // 忽略無效的層級值
            }
        }

        JPanel configPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 作者欄位
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        configPanel.add(new JLabel("作者:"), gbc);

        gbc.gridy = 1;
        JTextField authorField = new JTextField(author, 20);
        configPanel.add(authorField, gbc);

        // 主類名稱
        gbc.gridy = 2;
        configPanel.add(new JLabel("主類名稱:"), gbc);

        gbc.gridy = 3;
        JTextField mainClassField = new JTextField(mainClassName, 20);
        configPanel.add(mainClassField, gbc);

        // 為每個層級的每個對象創建類名輸入欄位
        Map<String, JTextField> objectClassFields = new HashMap<>();
        int currentRow = 4;

        for (Map.Entry<Integer, Set<String>> entry : levelObjects.entrySet()) {
            int level = entry.getKey();
            Set<String> objects = entry.getValue();

            if (!objects.isEmpty()) {
                // 添加層級標題
                gbc.gridy = currentRow++;
                gbc.gridwidth = 2;
                configPanel.add(new JLabel("第 " + level + " 層級:"), gbc);

                // 為該層級的每個對象添加輸入欄位
                for (String objectName : objects) {
                    gbc.gridy = currentRow++;
                    gbc.gridwidth = 1;
                    configPanel.add(new JLabel("    " + objectName + ":"), gbc);

                    String defaultClassName = levelClassNamesMap
                            .getOrDefault(level, new HashMap<>())
                            .getOrDefault(objectName, objectName + "DTO");
                    JTextField classField = new JTextField(defaultClassName, 20);
                    gbc.gridx = 1;
                    configPanel.add(classField, gbc);
                    objectClassFields.put(level + ":" + objectName, classField);
                    gbc.gridx = 0;
                }
            }
        }

        // 記住作者選項
        gbc.gridy = currentRow;
        gbc.gridwidth = 2;
        rememberAuthorCheckBox = new JCheckBox("記住作者", !author.isEmpty());
        configPanel.add(rememberAuthorCheckBox, gbc);

        // 創建一個帶滾動條的面板
        JScrollPane scrollPane = new JScrollPane(configPanel);
        scrollPane.setPreferredSize(new Dimension(400, Math.min(500, currentRow * 35)));

        if (JOptionPane.showConfirmDialog(null, scrollPane,
                "DTO配置", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

            author = authorField.getText().trim();
            mainClassName = mainClassField.getText().trim();

            // 清除舊的類名映射
            levelClassNamesMap.clear();

            // 保存每個對象的類名
            for (Map.Entry<String, JTextField> entry : objectClassFields.entrySet()) {
                String key = entry.getKey();
                String className = entry.getValue().getText().trim();

                if (!className.isEmpty()) {
                    String[] parts = key.split(":");
                    int level = Integer.parseInt(parts[0]);
                    String objectName = parts[1];

                    Map<String, String> levelMap = levelClassNamesMap.computeIfAbsent(level, k -> new HashMap<>());
                    levelMap.put(objectName, className);
                }
            }

            if (rememberAuthorCheckBox.isSelected()) {
                PropertiesComponent.getInstance().setValue(REMEMBERED_AUTHOR_KEY, author);
            } else {
                PropertiesComponent.getInstance().unsetValue(REMEMBERED_AUTHOR_KEY);
            }

            configurationDone = true;
        }
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
                boolean nullable = "Y".equalsIgnoreCase(tableModel.getValueAt(i, 4).toString());
                String comments = tableModel.getValueAt(i, 5).toString();

                fields.add(new DtoField(level, dataName, dataType, size, nullable, comments));
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
}
