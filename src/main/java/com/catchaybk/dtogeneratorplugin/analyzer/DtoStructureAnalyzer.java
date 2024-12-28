package com.catchaybk.dtogeneratorplugin.analyzer;

import com.catchaybk.dtogeneratorplugin.model.DtoField;
import com.catchaybk.dtogeneratorplugin.model.DtoStructure;
import java.util.*;

public class DtoStructureAnalyzer {
    private final List<DtoField> fields;

    public DtoStructureAnalyzer(List<DtoField> fields) {
        this.fields = fields;
    }

    public Map<Integer, List<DtoStructure>> analyze() {
        Map<Integer, List<DtoStructure>> structures = new HashMap<>();
        List<DtoField> mainFields = new ArrayList<>();
        List<DtoField> supListFields = new ArrayList<>();
        List<DtoField> subSeqnoListFields = new ArrayList<>();

        boolean inSupList = false;
        boolean inSubSeqnoList = false;
        String currentParentField = null;

        for (DtoField field : fields) {
            if (isSupListField(field)) {
                handleSupListTransition(structures, mainFields);
                inSupList = true;
                currentParentField = "SupList";
                mainFields.add(field);
                continue;
            }

            if (isSubSeqnoListField(field)) {
                handleSubSeqnoListTransition(structures, supListFields, currentParentField);
                inSubSeqnoList = true;
                supListFields.add(field);
                continue;
            }

            addFieldToAppropriateList(field, inSubSeqnoList, inSupList,
                    mainFields, supListFields, subSeqnoListFields);
        }

        // Handle remaining fields
        finalizeStructures(structures, mainFields, supListFields, subSeqnoListFields);

        return structures;
    }

    private boolean isSupListField(DtoField field) {
        return field.getDataName().equals("SupList") && field.getDataType().contains("List");
    }

    private boolean isSubSeqnoListField(DtoField field) {
        return field.getDataName().equals("SubSeqnoList") && field.getDataType().contains("List");
    }

    private void handleSupListTransition(Map<Integer, List<DtoStructure>> structures, List<DtoField> mainFields) {
        if (!mainFields.isEmpty()) {
            addStructure(structures, 0, null, new ArrayList<>(mainFields));
        }
    }

    private void handleSubSeqnoListTransition(Map<Integer, List<DtoStructure>> structures,
            List<DtoField> supListFields, String currentParentField) {
        if (!supListFields.isEmpty()) {
            addStructure(structures, 1, currentParentField, new ArrayList<>(supListFields));
            supListFields.clear();
        }
    }

    private void addFieldToAppropriateList(DtoField field, boolean inSubSeqnoList, boolean inSupList,
            List<DtoField> mainFields, List<DtoField> supListFields, List<DtoField> subSeqnoListFields) {
        if (inSubSeqnoList) {
            subSeqnoListFields.add(field);
        } else if (inSupList) {
            supListFields.add(field);
        } else {
            mainFields.add(field);
        }
    }

    private void finalizeStructures(Map<Integer, List<DtoStructure>> structures,
            List<DtoField> mainFields, List<DtoField> supListFields, List<DtoField> subSeqnoListFields) {
        if (!mainFields.isEmpty()) {
            addStructure(structures, 0, null, mainFields);
        }
        if (!supListFields.isEmpty()) {
            addStructure(structures, 1, "SupList", supListFields);
        }
        if (!subSeqnoListFields.isEmpty()) {
            addStructure(structures, 2, "SubSeqnoList", subSeqnoListFields);
        }
    }

    private void addStructure(Map<Integer, List<DtoStructure>> structures,
            int level, String parentField, List<DtoField> fields) {
        structures.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new DtoStructure(level, parentField, new ArrayList<>(fields)));
    }

    // ... 其他輔助方法
}