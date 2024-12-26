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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        List<String> cells = new ArrayList<>();
        StringBuilder currentCell = new StringBuilder();
        boolean inDataType = false;
        int fieldCount = 0;

        // 先將行按空格分割，但保持 List<Object> 這樣的類型完整
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean inComplexType = false;

        for (char c : row.toCharArray()) {
            if (c == '<') {
                inComplexType = true;
                token.append(c);
            } else if (c == '>') {
                inComplexType = false;
                token.append(c);
            } else if (Character.isWhitespace(c) && !inComplexType) {
                if (token.length() > 0) {
                    tokens.add(token.toString());
                    token = new StringBuilder();
                }
            } else {
                token.append(c);
            }
        }
        if (token.length() > 0) {
            tokens.add(token.toString());
        }

        // 處理分割後的標記
        for (String t : tokens) {
            if (fieldCount < 5) {
                // 處理前5個字段
                cells.add(t);
                fieldCount++;
            } else {
                // 收集剩餘的所有內容作為註釋
                if (currentCell.length() > 0) currentCell.append(" ");
                currentCell.append(t);
            }
        }

        // 如果收集到了完整的行
        if (fieldCount >= 3) { // 修改為3，因為size和nullable可能為空
            // 確保至少有5個欄位
            while (cells.size() < 5) {
                cells.add("");
            }
            // 添加註釋
            cells.add(currentCell.toString());
            addTableRow(cells.toArray(new String[0]));
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

        JPanel configPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 作者欄位
        gbc.gridx = 0;
        gbc.gridy = 0;
        configPanel.add(new JLabel("作者:"), gbc);

        JTextField authorField = new JTextField(author, 20);
        gbc.gridx = 1;
        configPanel.add(authorField, gbc);

        // 主類名稱
        gbc.gridx = 0;
        gbc.gridy = 1;
        configPanel.add(new JLabel("主類名稱:"), gbc);

        JTextField mainClassField = new JTextField(mainClassName, 20);
        gbc.gridx = 1;
        configPanel.add(mainClassField, gbc);

        // 記住作者選項
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        rememberAuthorCheckBox = new JCheckBox("記住作者", !author.isEmpty());
        configPanel.add(rememberAuthorCheckBox, gbc);
        if (JOptionPane.showConfirmDialog(null, configPanel,
                "DTO配置", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

            author = authorField.getText().trim();
            mainClassName = mainClassField.getText().trim();

            if (rememberAuthorCheckBox.isSelected()) {
                PropertiesComponent.getInstance().setValue(REMEMBERED_AUTHOR_KEY, author);
            } else {
                PropertiesComponent.getInstance().unsetValue(REMEMBERED_AUTHOR_KEY);
            }

            configurationDone = true;
        }
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
