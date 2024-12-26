package com.catchaybk.dtogeneratorplugin.ui;

import com.catchaybk.dtogeneratorplugin.model.DtoField;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class DtoGeneratorDialog extends DialogWrapper {
    private JBTable table;
    private DefaultTableModel tableModel;

    public DtoGeneratorDialog() {
        super(true);
        init();
        setTitle("DTO Generator");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        String[] columnNames = {"Level", "Data Name", "Data Type", "Size", "Nullable", "Comments"};
        tableModel = new DefaultTableModel(columnNames, 0);
        table = new JBTable(tableModel);

        // 註冊粘貼動作
        registerPasteAction();

        JBScrollPane scrollPane = new JBScrollPane(table);
        dialogPanel.add(scrollPane, BorderLayout.CENTER);

        // 添加按鈕面板
        JPanel buttonPanel = new JPanel();
        JButton addRowButton = new JButton("Add Row");
        JButton removeRowButton = new JButton("Remove Row");
        JButton pasteButton = new JButton("Paste");

        addRowButton.addActionListener(e -> addRow());
        removeRowButton.addActionListener(e -> removeSelectedRow());
        pasteButton.addActionListener(e -> handlePaste());

        buttonPanel.add(addRowButton);
        buttonPanel.add(removeRowButton);
        buttonPanel.add(pasteButton);
        dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialogPanel.setPreferredSize(new Dimension(800, 400));
        return dialogPanel;
    }

    private void registerPasteAction() {
        // 註冊 Ctrl+V 快捷鍵
        KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        table.registerKeyboardAction(e -> handlePaste(), "Paste",
                paste, JComponent.WHEN_FOCUSED);
    }

    private void handlePaste() {
        try {
            String clipboardData = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);

            String[] rows = clipboardData.split("\n");
            for (String row : rows) {
                // 使用正則表達式來分割數據，處理多個空格的情況
                String[] parts = row.trim().split("\\s+");
                if (parts.length >= 5) {
                    // 提取前5個字段
                    String level = parts[0];
                    String dataName = parts[1];
                    String dataType = parts[2];
                    String size = parts[3];
                    String nullable = parts[4];

                    // 將剩餘部分作為註釋
                    StringBuilder comments = new StringBuilder();
                    for (int i = 5; i < parts.length; i++) {
                        if (i > 5) comments.append(" ");
                        comments.append(parts[i]);
                    }

                    // 添加到表格
                    tableModel.addRow(new Object[]{
                            level,
                            dataName,
                            dataType,
                            size,
                            nullable,
                            comments.toString()
                    });
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addRow() {
        tableModel.addRow(new Object[]{"", "", "", "", "", ""});
    }

    private void removeSelectedRow() {
        int[] selectedRows = table.getSelectedRows();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            tableModel.removeRow(selectedRows[i]);
        }
    }

    public List<DtoField> getDtoFields() {
        List<DtoField> fields = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                String levelStr = String.valueOf(tableModel.getValueAt(i, 0));
                String dataName = String.valueOf(tableModel.getValueAt(i, 1));
                String dataType = String.valueOf(tableModel.getValueAt(i, 2));
                String size = String.valueOf(tableModel.getValueAt(i, 3));
                String nullableStr = String.valueOf(tableModel.getValueAt(i, 4));
                String comments = String.valueOf(tableModel.getValueAt(i, 5));

                // 轉換數據
                int level = Integer.parseInt(levelStr.trim());
                boolean nullable = "Y".equalsIgnoreCase(nullableStr.trim());

                if (!dataName.isEmpty() && !dataType.isEmpty()) {
                    fields.add(new DtoField(level, dataName, dataType, size, nullable, comments));
                }
            } catch (Exception e) {
                // 跳過無效的行
                continue;
            }
        }
        return fields;
    }

    @Override
    protected void doOKAction() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null,
                    "Please add at least one field.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        super.doOKAction();
    }
}