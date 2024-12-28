package com.catchaybk.dtogeneratorplugin.ui.model;

import com.catchaybk.dtogeneratorplugin.model.DtoField;
import javax.swing.table.DefaultTableModel;
import java.util.*;

public class DtoTableModel extends DefaultTableModel {
    private static final String[] COLUMN_NAMES = { "Level", "Data Name", "Data Type", "Size", "Required", "Comments" };

    public DtoTableModel() {
        super(COLUMN_NAMES, 0);
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
        int currentColumn = 0;
        StringBuilder comment = new StringBuilder();
        boolean isComment = false;

        for (String part : parts) {
            if (part.trim().isEmpty())
                continue;

            if (isComment || currentColumn > 4) {
                appendToComment(comment, part);
                isComment = true;
            } else {
                currentColumn = processColumn(columns, currentColumn, part, comment);
                if (currentColumn == -1) {
                    isComment = true;
                }
            }
        }

        if (comment.length() > 0) {
            columns[5] = comment.toString().trim();
        }

        return columns;
    }

    private void appendToComment(StringBuilder comment, String part) {
        if (comment.length() > 0) {
            comment.append(" ");
        }
        comment.append(part.trim());
    }

    private int processColumn(String[] columns, int currentColumn, String part, StringBuilder comment) {
        String trimmedPart = part.trim();

        switch (currentColumn) {
            case 3: // Size
                if (trimmedPart.matches("\\d+")) {
                    columns[currentColumn] = trimmedPart;
                    return currentColumn + 1;
                }
                appendToComment(comment, part);
                return -1;

            case 4: // Required
                if (trimmedPart.matches("[YN-]")) {
                    columns[currentColumn] = trimmedPart;
                    return currentColumn + 1;
                }
                appendToComment(comment, part);
                return -1;

            default:
                columns[currentColumn] = trimmedPart;
                return currentColumn + 1;
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
                "Y".equalsIgnoreCase(requiredStr), comments);
        field.setRequiredString(requiredStr);
        return field;
    }

    public boolean validateDataTypes() {
        for (int i = 0; i < getRowCount(); i++) {
            String dataType = (String) getValueAt(i, 2);
            if (dataType == null || dataType.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}