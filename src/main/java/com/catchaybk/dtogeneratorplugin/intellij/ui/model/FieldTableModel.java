package com.catchaybk.dtogeneratorplugin.intellij.ui.model;

import com.catchaybk.dtogeneratorplugin.core.config.TypeRegistry;
import com.catchaybk.dtogeneratorplugin.core.model.Field;
import com.intellij.openapi.ui.Messages;
import org.codehaus.plexus.util.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * 字段表格數據模型
 * 負責管理和驗證表格中的字段數據，並處理數據的智能填充
 * <p>
 * 主要功能：
 * 1. 管理表格數據的增刪改查
 * 2. 處理數據的驗證和格式化
 * 3. 提供表格單元格的渲染邏輯
 * 4. 處理剪貼板數據的智能填充
 * 5. 支持列順序的動態調整
 * <p>
 * 數據填充策略：
 * - 3個值或更少：按順序填充到前面的欄位
 * - 4個值：根據最後一個值的特徵（Size/Required/Comment）決定填充位置
 * - 5個值：分析第四和第五個值的特徵來決定填充位置
 * - 6個值：按順序填充所有欄位
 */
public class FieldTableModel extends DefaultTableModel {
    // 使用 enum 來定義列名，方便管理和查找
    public enum Column {
        LEVEL("Level"),
        DATA_NAME("Data Name"),
        DATA_TYPE("Data Type"),
        SIZE("Size"),
        REQUIRED("Required"),
        COMMENTS("Comments"),
        PATTERN("Pattern");

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

    private static final String[] COLUMN_NAMES = {
            "Level", "Data Name", "Data Type", "Size", "Required", "Comments", "Pattern"
    };

    private final Set<String> warnedTypes = new HashSet<>(); // 記錄已經警告過的類型
    private boolean isJava17;
    private List<String> currentColumnOrder;

    public FieldTableModel(boolean isJava17) {
        super(COLUMN_NAMES, 0);
        this.isJava17 = isJava17;
    }

    public void addEmptyRow() {
        addRow(new Object[] { "", "", "", "", "", "", "" });
    }

    public void processClipboardData(String clipboardData) {
        Arrays.stream(clipboardData.split("\n"))
                .map(String::trim)
                .filter(row -> !row.isEmpty())
                .forEach(this::processRow);
    }

    /**
     * 處理從剪貼板貼上的數據
     * 支持多行數據和狀態說明行
     *
     * @param row 要處理的數據行
     */
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

        if (values.isEmpty())
            return;

        // 創建新行數據
        String[] newRow = new String[getColumnCount()];
        Arrays.fill(newRow, "");

        // 智能分配數據到對應欄位
        distributeValues(values, newRow);

        // 如果有層級數字，則添加行
        if (Arrays.stream(newRow).anyMatch(value -> value.matches("\\d+"))) {
            addRow(newRow);
        }
    }

    /**
     * 根據數據量和特徵智能分配值到對應欄位
     *
     * @param values 要分配的值列表
     * @param newRow 新行數據數組
     */
    private void distributeValues(List<String> values, String[] newRow) {
        int totalValues = values.size();

        // 根據值的數量決定處理策略
        switch (totalValues) {
            case 1:
            case 2:
            case 3:
                // 3個或更少的值，直接按順序填充前面的欄位
                fillByOrder(values, newRow);
                break;

            case 4:
                handleFourValues(values, newRow);
                break;
            case 5:
                handleFiveValues(values, newRow);
                break;
            case 6:
                // 6個值的情況，Pattern欄位保持為空
                handleNormalValues(values, newRow);
                break;

            default:
                // 7個或更多值，最後一個值作為Pattern
                handleValuesWithPattern(values, newRow);
        }
    }

    private void fillByOrder(List<String> values, String[] newRow) {
        // 直接按順序填充
        for (int i = 0; i < values.size(); i++) {
            newRow[i] = values.get(i);
        }
    }

    /**
     * 處理4個值的情況
     * 規則：
     * 1. 如果最後一個值是數字格式，視為Size
     * 2. 如果最後一個值是Y/N，視為Required
     * 3. 其他情況，視為Comments
     *
     * @param values 4個值的列表
     * @param newRow 新行數據數組
     */
    private void handleFourValues(List<String> values, String[] newRow) {
        String lastValue = values.get(3);

        // 如果最後一個值是數字格式，可能是Size
        if (isValidSizeFormat(lastValue)) {
            newRow[0] = values.get(0); // Level
            newRow[1] = values.get(1); // Data Name
            newRow[2] = values.get(2); // Data Type
            newRow[3] = lastValue; // Size
        }
        // 如果最後一個值是Y/N，可能是Required
        else if (lastValue.matches("[YyNn]")) {
            newRow[0] = values.get(0); // Level
            newRow[1] = values.get(1); // Data Name
            newRow[2] = values.get(2); // Data Type
            newRow[4] = lastValue.toUpperCase(); // Required
        }
        // 其他情況，可能是註解
        else {
            newRow[0] = values.get(0); // Level
            newRow[1] = values.get(1); // Data Name
            newRow[2] = values.get(2); // Data Type
            newRow[5] = lastValue; // Comments
        }
    }

    /**
     * 處理5個值的情況
     * 規則：
     * 1. 前三個值固定為Level、Name、Type
     * 2. 第四個值如果是Size格式，則第五個值可能是Required或Comment
     * 3. 第四個值如果是Y/N，則是Required，第五個值為Comment
     * 4. 其他情況，第四個值為Size，第五個值為Comment
     *
     * @param values 5個值的列表
     * @param newRow 新行數據數組
     */
    private void handleFiveValues(List<String> values, String[] newRow) {
        String fourthValue = values.get(3);
        String fifthValue = values.get(4);

        // 基本資料總是填入
        newRow[0] = values.get(0); // Level
        newRow[1] = values.get(1); // Data Name
        newRow[2] = values.get(2); // Data Type

        // 如果第四個值是Size格式
        if (isValidSizeFormat(fourthValue)) {
            newRow[3] = fourthValue; // Size
            if (fifthValue.matches("[YyNn]")) {
                newRow[4] = fifthValue.toUpperCase(); // Required
            } else {
                newRow[5] = fifthValue; // Comments
            }
        }
        // 如果第四個值是Y/N
        else if (fourthValue.matches("[YyNn]")) {
            newRow[4] = fourthValue.toUpperCase(); // Required
            newRow[5] = fifthValue; // Comments
        }
        // 其他情況
        else {
            newRow[3] = fourthValue; // Size
            newRow[5] = fifthValue; // Comments
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
                Map<String, String> fieldData = new HashMap<>();

                // 使用當前列順序來獲取數據
                for (int viewIndex = 0; viewIndex < getColumnCount(); viewIndex++) {
                    String columnName = getColumnName(viewIndex);
                    String value = getValueAt(i, viewIndex).toString();
                    fieldData.put(columnName, value);
                }

                // 創建 Field 對象
                Field field = new Field(
                        Integer.parseInt(fieldData.getOrDefault("Level", "1")),
                        fieldData.getOrDefault("Data Name", ""),
                        fieldData.getOrDefault("Data Type", "String"),
                        fieldData.getOrDefault("Size", ""),
                        "Y".equalsIgnoreCase(fieldData.getOrDefault("Required", "N")),
                        fieldData.getOrDefault("Comments", ""),
                        fieldData.getOrDefault("Pattern", ""),
                        isJava17);
                fields.add(field);
            } catch (Exception e) {
                // Skip invalid rows
            }
        }
        return fields;
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
            if (!isValidSizeFormat(size)) {
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

    /**
     * 驗證Size格式是否有效
     * 支持：
     * 1. 純數字格式（如：10）
     * 2. 帶小數點的格式（如：10,2）
     *
     * @param value 要驗證的值
     * @return 格式是否有效
     */
    private boolean isValidSizeFormat(String value) {
        if (StringUtils.isBlank(value)) {
            return true;
        }

        // 檢查是否為純數字或帶逗號的數字格式
        return value.matches("\\d+") || value.matches("\\d+,\\d+");
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

    /**
     * 表格單元格渲染器
     * 負責：
     * 1. 數據類型的有效性驗證和提示
     * 2. Size格式的驗證和提示
     * 3. 錯誤和警告的視覺反饋
     */
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

                if (!cellValue.isEmpty() && !model.isValidSizeFormat(cellValue)) {
                    setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    setToolTipText("Size格式不正確");
                    setBackground(new Color(255, 200, 200));
                } else {
                    setBorder(null);
                    setToolTipText(null);
                    setBackground(table.getBackground());
                }
            } else if (columnName.equals("Pattern")) {
                // Pattern 欄位不允許拖動，總是最後一列
                if (column != table.getColumnCount() - 1) {
                    setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    setToolTipText("Pattern 欄位必須是最後一列");
                }
            } else {
                setBorder(null);
                setToolTipText(null);
                setBackground(table.getBackground());
            }

            return c;
        }
    }

    /**
     * 更新列順序
     * 當用戶拖動列時調用，確保數據處理跟隨新的列順序
     */
    public void updateColumnOrder(TableColumnModel columnModel) {
        currentColumnOrder = new ArrayList<>();
        for (int viewIndex = 0; viewIndex < columnModel.getColumnCount(); viewIndex++) {
            int modelIndex = columnModel.getColumn(viewIndex).getModelIndex();
            currentColumnOrder.add(COLUMN_NAMES[modelIndex]);
        }
    }

    private void handleValuesWithPattern(List<String> values, String[] newRow) {
        // 填充前6個欄位
        for (int i = 0; i < 6; i++) {
            newRow[i] = values.get(i);
        }
        // 最後一個值作為Pattern
        newRow[6] = values.get(values.size() - 1);
    }

    private void handleNormalValues(List<String> values, String[] newRow) {
        // 填充前6個欄位
        for (int i = 0; i < Math.min(values.size(), 6); i++) {
            newRow[i] = values.get(i);
        }
        // Pattern 欄位保持為空
        newRow[6] = "";
    }
}