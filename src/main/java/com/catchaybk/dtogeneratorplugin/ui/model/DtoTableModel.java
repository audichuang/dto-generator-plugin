package com.catchaybk.dtogeneratorplugin.ui.model;

import com.catchaybk.dtogeneratorplugin.config.DataTypeConfig;
import com.catchaybk.dtogeneratorplugin.model.DtoField;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.*;

public class DtoTableModel extends DefaultTableModel {
    private static final String[] COLUMN_NAMES = {"Level", "Data Name", "Data Type", "Size", "Required", "Comments"};

    private final Set<String> warnedTypes = new HashSet<>(); // 記錄已經警告過的類型
    private boolean isJava17;

    public DtoTableModel(boolean isJava17) {
        super(COLUMN_NAMES, 0);
        this.isJava17 = isJava17;
    }

    public void addEmptyRow() {
        addRow(new Object[]{"", "", "", "", "", ""});
    }

    public void processClipboardData(String clipboardData) {
        Arrays.stream(clipboardData.split("\n"))
                .map(String::trim)
                .filter(row -> !row.isEmpty())
                .forEach(this::processRow);
    }

    private void processRow(String row) {
        // 檢查是否是狀態說明行
        if (row.matches("^\\d+:\\s+.*")) {
            appendToLastRowComment(row);
            return;
        }

        String[] columns = parseRowData(row);
        if (isValidRow(columns)) {
            addRow(columns);
        }
    }

    private void appendToLastRowComment(String comment) {
        int lastRow = getRowCount() - 1;
        if (lastRow >= 0) {
            String currentComment = (String) getValueAt(lastRow, 5);
            setValueAt(currentComment + "\n" + comment.trim(), lastRow, 5);
        }
    }

    private String[] parseRowData(String row) {
        String[] columns = new String[6];
        Arrays.fill(columns, "");

        String[] parts = row.split("\\t|(?:  +)");

        // 找到第一個數字（level）的位置
        int levelPos = -1;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].trim().matches("\\d+")) {
                levelPos = i;
                break;
            }
        }

        if (levelPos == -1)
            return columns;

        // 根據當前列的順序填充數據
        for (int i = levelPos; i < parts.length && i - levelPos < 6; i++) {
            String part = parts[i].trim();
            if (part.isEmpty())
                continue;

            int relativePos = i - levelPos;
            columns[getColumnByPosition(relativePos)] = part;
        }

        return columns;
    }

    private int getColumnByPosition(int pos) {
        switch (pos) {
            case 0:
                return 0; // Level 永遠是第一個
            case 1:
                return 1; // Data Name 永遠是第二個
            case 2:
                return 2; // Data Type
            case 3:
                return 3; // Size
            case 4:
                return 4; // Required
            case 5:
                return 5; // Comments
            default:
                return -1;
        }
    }

    private boolean isValidRow(String[] columns) {
        return !columns[0].isEmpty() && !columns[1].isEmpty();
    }

    public List<DtoField> getDtoFields() {
        List<DtoField> fields = new ArrayList<>();
        for (int i = 0; i < getRowCount(); i++) {
            try {
                fields.add(createDtoField(i));
            } catch (Exception e) {
                // Skip invalid rows
            }
        }
        return fields;
    }

    private DtoField createDtoField(int row) {
        int level = Integer.parseInt(getValueAt(row, 0).toString());
        String dataName = getValueAt(row, 1).toString();
        String dataType = getValueAt(row, 2).toString();
        String size = getValueAt(row, 3).toString();
        String requiredStr = getValueAt(row, 4).toString().trim();
        String comments = getValueAt(row, 5).toString();

        DtoField field = new DtoField(level, dataName, dataType, size,
                "Y".equalsIgnoreCase(requiredStr), comments, isJava17);
        field.setRequiredString(requiredStr);
        return field;
    }

    /**
     * 驗證所有數據類型和Size格式
     *
     * @return null 如果有錯誤，否則返回未知類型的集合
     */
    public Set<String> validateTypes() {
        boolean hasEmptyTypes = false;
        boolean hasSizeError = false;
        Set<String> unknownTypesList = new HashSet<>();

        // 獲取 Data Type 和 Size 列的索引
        int dataTypeIndex = Arrays.asList(COLUMN_NAMES).indexOf("Data Type");
        int sizeIndex = Arrays.asList(COLUMN_NAMES).indexOf("Size");

        for (int i = 0; i < getRowCount(); i++) {
            String dataType = (String) getValueAt(i, dataTypeIndex);
            String size = (String) getValueAt(i, sizeIndex);

            // 檢查必填的 Data Type
            if (dataType == null || dataType.trim().isEmpty()) {
                hasEmptyTypes = true;
            } else if (!DataTypeConfig.isKnownType(dataType)) {
                unknownTypesList.add(dataType);
            }

            // 檢查 Size 格式
            if (!isValidSizeFormat(size, dataType)) {
                hasSizeError = true;
            }
        }

        if (hasEmptyTypes) {
            Messages.showErrorDialog("有欄位的數據類型為空，請填寫所有數據類型", "錯誤");
            return null;
        }

        if (hasSizeError) {
            Messages.showErrorDialog("Size欄位必須是數字，請修正後再試", "錯誤");
            return null;
        }

        // 過濾掉已經警告過的類型
        unknownTypesList.removeAll(warnedTypes);
        return unknownTypesList;
    }

    // 修改原有的 validateDataTypes 方法
    public boolean validateDataTypes() {
        Set<String> unknownTypes = validateTypes();
        if (unknownTypes == null) {
            return false;
        }
        if (!unknownTypes.isEmpty()) {
            warnedTypes.addAll(unknownTypes);
        }
        return true;
    }

    public static class ValidationCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            DtoTableModel model = (DtoTableModel) table.getModel();
            String cellValue = value != null ? value.toString().trim() : "";
            String columnName = table.getColumnName(column);

            if (columnName.equals("Data Type")) {
                if (cellValue.isEmpty()) {
                    setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    setToolTipText("數據類型不能為空（必填）");
                } else if (!DataTypeConfig.isKnownType(cellValue)) {
                    setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
                    setToolTipText("未知的數據類型：" + cellValue + "（可能是新類型或輸入錯誤）");
                } else {
                    setBorder(null);
                    setToolTipText(null);
                }
            } else if (columnName.equals("Size")) {
                // 獲取當前行的 Data Type
                String dataType = (String) table.getValueAt(row,
                        Arrays.asList(COLUMN_NAMES).indexOf("Data Type"));

                if (!cellValue.isEmpty() && !model.isValidSizeFormat(cellValue, dataType)) {
                    setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    setToolTipText("Size格式不正確");
                    setBackground(new Color(255, 200, 200));
                } else {
                    setBorder(null);
                    setToolTipText(null);
                    setBackground(table.getBackground());
                }
            } else {
                setBorder(null);
                setToolTipText(null);
                setBackground(table.getBackground());
            }

            return c;
        }
    }

    private boolean isValidSizeFormat(String size, String dataType) {
        if (size == null || size.trim().isEmpty()) {
            return true;
        }

        String lowerType = dataType.toLowerCase();
        // 對於 decimal 類型，允許 "整數位,小數位" 格式
        if (lowerType.equals("decimal") || lowerType.equals("bigdecimal")) {
            return size.matches("\\d+") || size.matches("\\d+,\\d+");
        }
        // 其他類型只允許純數字
        return size.matches("\\d+");
    }

    public void updateJavaVersion(boolean isJava17) {
        this.isJava17 = isJava17;
    }
}