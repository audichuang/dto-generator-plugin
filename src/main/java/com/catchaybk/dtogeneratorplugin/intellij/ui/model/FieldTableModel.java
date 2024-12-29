package com.catchaybk.dtogeneratorplugin.intellij.ui.model;

import com.catchaybk.dtogeneratorplugin.core.config.TypeRegistry;
import com.catchaybk.dtogeneratorplugin.core.model.Field;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * 字段表格數據模型
 * 負責管理和驗證表格中的字段數據
 * <p>
 * 主要功能：
 * 1. 管理表格數據的增刪改查
 * 2. 處理數據的驗證和格式化
 * 3. 提供表格單元格的渲染邏輯
 * 4. 處理剪貼板數據的導入
 * <p>
 * 表格列：
 * - Level: 字段層級
 * - Data Name: 字段名稱
 * - Data Type: 數據類型
 * - Size: 大小限制
 * - Required: 是否必填
 * - Comments: 註解說明
 */
public class FieldTableModel extends DefaultTableModel {
    // 使用 enum 來定義列名，方便管理和查找
    public enum Column {
        LEVEL("Level"),
        DATA_NAME("Data Name"),
        DATA_TYPE("Data Type"),
        SIZE("Size"),
        REQUIRED("Required"),
        COMMENTS("Comments");

        private final String name;

        Column(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        // 根據列名找到對應的 enum
        public static Column fromName(String name) {
            for (Column col : values()) {
                if (col.getName().equals(name)) {
                    return col;
                }
            }
            return null;
        }
    }

    private static final String[] COLUMN_NAMES = { "Level", "Data Name", "Data Type", "Size", "Required", "Comments" };

    private final Set<String> warnedTypes = new HashSet<>(); // 記錄已經警告過的類型
    private boolean isJava17;
    private List<String> currentColumnOrder;

    public FieldTableModel(boolean isJava17) {
        super(COLUMN_NAMES, 0);
        this.isJava17 = isJava17;
    }

    public void addEmptyRow() {
        addRow(new Object[] { "", "", "", "", "", "" });
    }

    public void processClipboardData(String clipboardData) {
        Arrays.stream(clipboardData.split("\n"))
                .map(String::trim)
                .filter(row -> !row.isEmpty())
                .forEach(this::processRow);
    }

    private void processRow(String row) {
        if (row.matches("^\\d+:\\s+.*")) {
            appendToLastRowComment(row);
            return;
        }

        // 分割輸入數據
        String[] parts = row.split("\\t|(?:  +)");
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                values.add(part.trim());
            }
        }

        // 如果沒有數據，直接返回
        if (values.isEmpty()) {
            return;
        }

        // 創建新行數據
        String[] newRow = new String[getColumnCount()];
        Arrays.fill(newRow, "");

        // 使用當前的列順序填充數據
        if (currentColumnOrder != null) {
            for (int i = 0; i < Math.min(values.size(), getColumnCount()); i++) {
                // 根據當前列順序找到正確的位置
                String columnName = currentColumnOrder.get(i);
                int modelIndex = Arrays.asList(COLUMN_NAMES).indexOf(columnName);
                if (modelIndex >= 0 && i < values.size()) {
                    newRow[modelIndex] = values.get(i);
                }
            }
        } else {
            // 如果還沒有列順序（第一次使用），就按順序填充
            for (int i = 0; i < Math.min(values.size(), getColumnCount()); i++) {
                newRow[i] = values.get(i);
            }
        }

        // 檢查是否有效（至少包含一個數字）
        boolean hasNumber = false;
        for (String value : newRow) {
            if (value.matches("\\d+")) {
                hasNumber = true;
                break;
            }
        }

        if (hasNumber) {
            addRow(newRow);
        }
    }

    private void appendToLastRowComment(String comment) {
        int lastRow = getRowCount() - 1;
        if (lastRow >= 0) {
            String currentComment = (String) getValueAt(lastRow, 5);
            setValueAt(currentComment + "\n" + comment.trim(), lastRow, 5);
        }
    }

    private boolean isValidRow(String[] columns) {
        // 檢查是否有 Level 和 Data Name
        boolean hasLevel = false;
        boolean hasName = false;

        for (int i = 0; i < getColumnCount(); i++) {
            String columnName = getColumnName(i);
            if (columnName.equals("Level") && !columns[i].isEmpty()) {
                hasLevel = true;
            }
            if (columnName.equals("Data Name") && !columns[i].isEmpty()) {
                hasName = true;
            }
        }

        return hasLevel && hasName;
    }

    public List<Field> getDtoFields() {
        List<Field> fields = new ArrayList<>();
        for (int i = 0; i < getRowCount(); i++) {
            try {
                fields.add(createDtoField(i));
            } catch (Exception e) {
                // Skip invalid rows
            }
        }
        return fields;
    }

    private Field createDtoField(int row) {
        Map<String, String> fieldData = new HashMap<>();

        // 使用當前的列順序獲取數據
        if (currentColumnOrder != null) {
            for (int i = 0; i < getColumnCount(); i++) {
                String columnName = currentColumnOrder.get(i);
                // 找到對應的模型索引
                int modelIndex = Arrays.asList(COLUMN_NAMES).indexOf(columnName);
                String value = getValueAt(row, modelIndex).toString();
                fieldData.put(columnName, value);
            }
        } else {
            // 如果沒有自定義順序，使用默認順序
            for (int i = 0; i < getColumnCount(); i++) {
                String columnName = COLUMN_NAMES[i];
                String value = getValueAt(row, i).toString();
                fieldData.put(columnName, value);
            }
        }

        return new Field(
                Integer.parseInt(fieldData.getOrDefault("Level", "1")),
                fieldData.getOrDefault("Data Name", ""),
                fieldData.getOrDefault("Data Type", "String"),
                fieldData.getOrDefault("Size", ""),
                "Y".equalsIgnoreCase(fieldData.getOrDefault("Required", "N")),
                fieldData.getOrDefault("Comments", ""),
                isJava17);
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

        // 使用當前列順序獲取索引
        int dataTypeIndex = -1;
        int sizeIndex = -1;

        if (currentColumnOrder != null) {
            for (int i = 0; i < currentColumnOrder.size(); i++) {
                if (currentColumnOrder.get(i).equals("Data Type")) {
                    // 找到對應的模型索引
                    dataTypeIndex = Arrays.asList(COLUMN_NAMES).indexOf("Data Type");
                } else if (currentColumnOrder.get(i).equals("Size")) {
                    // 找到對應的模型索引
                    sizeIndex = Arrays.asList(COLUMN_NAMES).indexOf("Size");
                }
            }
        } else {
            dataTypeIndex = Arrays.asList(COLUMN_NAMES).indexOf("Data Type");
            sizeIndex = Arrays.asList(COLUMN_NAMES).indexOf("Size");
        }

        for (int i = 0; i < getRowCount(); i++) {
            String dataType = dataTypeIndex >= 0 ? (String) getValueAt(i, dataTypeIndex) : "";
            String size = sizeIndex >= 0 ? (String) getValueAt(i, sizeIndex) : "";

            // 檢查必填的 Data Type
            if (dataType == null || dataType.trim().isEmpty()) {
                hasEmptyTypes = true;
            } else if (!TypeRegistry.isKnownType(dataType)) {
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

    // 獲取列索引的輔助方法
    private int getColumnIndex(Column column) {
        for (int i = 0; i < getColumnCount(); i++) {
            if (getColumnName(i).equals(column.getName())) {
                return i;
            }
        }
        return -1;
    }

    public static class ValidationCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            FieldTableModel model = (FieldTableModel) table.getModel();
            String cellValue = value != null ? value.toString().trim() : "";
            String columnName = table.getColumnName(column);

            if (columnName.equals("Data Type")) {
                if (cellValue.isEmpty()) {
                    setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    setToolTipText("數據類型不能為空（必填）");
                } else if (!TypeRegistry.isKnownType(cellValue)) {
                    setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
                    setToolTipText("未知的數據類型：" + cellValue + "（可能是新類型或輸入錯誤）");
                } else {
                    setBorder(null);
                    setToolTipText(null);
                }
            } else if (columnName.equals("Size")) {
                // 獲取當前行的 Data Type
                int dataTypeColumn = -1;
                for (int i = 0; i < table.getColumnCount(); i++) {
                    if (table.getColumnName(i).equals("Data Type")) {
                        dataTypeColumn = i;
                        break;
                    }
                }

                String dataType = dataTypeColumn >= 0 ? (String) table.getValueAt(row, dataTypeColumn) : "";

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

    public void updateColumnOrder(TableColumnModel columnModel) {
        currentColumnOrder = new ArrayList<>();
        for (int viewIndex = 0; viewIndex < columnModel.getColumnCount(); viewIndex++) {
            // 使用視圖索引獲取列名
            int modelIndex = columnModel.getColumn(viewIndex).getModelIndex();
            currentColumnOrder.add(COLUMN_NAMES[modelIndex]);
        }
    }

    private int getActualColumnIndex(String columnName) {
        if (currentColumnOrder != null) {
            return currentColumnOrder.indexOf(columnName);
        }
        return Arrays.asList(COLUMN_NAMES).indexOf(columnName);
    }
}