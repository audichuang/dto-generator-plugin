package com.catchaybk.dtogeneratorplugin;

import com.catchaybk.dtogeneratorplugin.model.DtoField;
import com.catchaybk.dtogeneratorplugin.model.DtoStructure;

import java.util.*;

/**
 * DTO結構分析器
 * 負責分析DTO字段之間的層級關係並建立結構樹
 */
public class DtoStructureAnalyzer {
    private final List<DtoField> allFields;
    private final String mainClassName;
    private final Map<Integer, Map<String, String>> levelClassNamesMap;

    public DtoStructureAnalyzer(List<DtoField> allFields, String mainClassName,
                                Map<Integer, Map<String, String>> levelClassNamesMap) {
        this.allFields = allFields;
        this.mainClassName = mainClassName;
        this.levelClassNamesMap = levelClassNamesMap;
    }

    public DtoStructure analyze() {
        int minLevel = findMinLevel();
        DtoStructure mainStructure = new DtoStructure(mainClassName);
        Map<Integer, Map<String, DtoStructure>> levelStructures = initializeLevelStructures(minLevel, mainStructure);
        processFieldsByLevel(levelStructures, minLevel);
        return mainStructure;
    }

    private int findMinLevel() {
        return allFields.stream()
                .mapToInt(DtoField::getLevel)
                .min()
                .orElse(1);
    }

    private Map<Integer, Map<String, DtoStructure>> initializeLevelStructures(int minLevel,
                                                                              DtoStructure mainStructure) {
        Map<Integer, Map<String, DtoStructure>> levelStructures = new HashMap<>();
        Map<String, DtoStructure> currentLevelStructures = new HashMap<>();
        levelStructures.put(minLevel, currentLevelStructures);
        currentLevelStructures.put("main", mainStructure);
        return levelStructures;
    }

    private void processFieldsByLevel(Map<Integer, Map<String, DtoStructure>> levelStructures, int minLevel) {
        Map<Integer, List<DtoField>> levelFields = groupFieldsByLevel();
        Set<Integer> levels = new TreeSet<>(levelFields.keySet());

        for (Integer level : levels) {
            processLevelFields(level, levelFields.get(level), levelStructures, minLevel);
        }
    }

    private Map<Integer, List<DtoField>> groupFieldsByLevel() {
        Map<Integer, List<DtoField>> levelFields = new HashMap<>();
        for (DtoField field : allFields) {
            levelFields.computeIfAbsent(field.getLevel(), k -> new ArrayList<>()).add(field);
        }
        return levelFields;
    }

    private void processLevelFields(Integer level, List<DtoField> fields,
                                    Map<Integer, Map<String, DtoStructure>> levelStructures, int minLevel) {
        if (fields == null)
            return;

        Map<String, DtoStructure> currentLevelStructures = levelStructures.computeIfAbsent(level, k -> new HashMap<>());

        for (DtoField field : fields) {
            DtoStructure parentStructure = findParentStructure(field, level, minLevel, levelStructures);
            if (parentStructure == null)
                continue;

            if (shouldCreateNewStructure(field)) {
                processComplexField(field, level, parentStructure, currentLevelStructures);
            } else if (field.isList()) {
                updateSimpleListDataType(field);
            }
            parentStructure.addField(field);
        }
    }

    private DtoStructure findParentStructure(DtoField field, Integer level, int minLevel,
                                             Map<Integer, Map<String, DtoStructure>> levelStructures) {
        if (level == minLevel) {
            return levelStructures.get(minLevel).get("main");
        }

        DtoField parentField = findParentField(field);
        if (parentField == null)
            return null;

        Map<String, DtoStructure> parentLevelStructures = levelStructures.get(level - 1);
        return parentLevelStructures.get(parentField.getDataName());
    }

    private DtoField findParentField(DtoField currentField) {
        int currentIndex = allFields.indexOf(currentField);
        int targetLevel = currentField.getLevel() - 1;

        for (int i = currentIndex - 1; i >= 0; i--) {
            DtoField field = allFields.get(i);
            if (field.getLevel() == targetLevel && (field.isObject() || field.isList())) {
                return field;
            }
            if (field.getLevel() < targetLevel) {
                break;
            }
        }
        return null;
    }

    private void processComplexField(DtoField field, Integer level,
                                     DtoStructure parentStructure, Map<String, DtoStructure> currentLevelStructures) {
        String className = determineClassName(field, level);
        field.setChildClassName(className);
        updateFieldDataType(field);

        DtoStructure childStructure = new DtoStructure(className);
        parentStructure.addChildStructure(childStructure, field);
        currentLevelStructures.put(field.getDataName(), childStructure);
    }

    private String determineClassName(DtoField field, Integer level) {
        Map<String, String> levelMap = levelClassNamesMap.get(level);
        if (levelMap != null) {
            String configuredClassName = levelMap.get(field.getDataName());
            if (configuredClassName != null && !configuredClassName.isEmpty()) {
                return configuredClassName;
            }
        }

        if (field.getDataName().equals("SupList")) {
            return levelClassNamesMap.get(1).get("SupList");
        }
        if (field.getDataName().equals("SubSeqnoList")) {
            return levelClassNamesMap.get(2).get("SubSeqnoList");
        }

        return field.getDataName() + "DTO";
    }

    private void updateFieldDataType(DtoField field) {
        if (field.isList()) {
            field.setDataType("List<" + field.getChildClassName() + ">");
        } else {
            field.setDataType(field.getChildClassName());
        }
    }

    private boolean shouldCreateNewStructure(DtoField field) {
        if (!field.isObject() && !field.isList()) {
            return false;
        }

        if (field.isList()) {
            String genericType = extractGenericType(field.getDataType());
            return !isSimpleType(genericType);
        }

        return field.isObject();
    }

    private String extractGenericType(String dataType) {
        if (dataType.startsWith("List<") && dataType.endsWith(">")) {
            return dataType.substring(5, dataType.length() - 1).trim();
        }
        return dataType;
    }

    private boolean isSimpleType(String type) {
        Set<String> simpleTypes = new HashSet<>(Arrays.asList(
                "String", "Integer", "Long", "Double", "Float", "Boolean",
                "Short", "Byte", "Character", "int", "long", "double",
                "float", "boolean", "short", "byte", "char"));

        simpleTypes.addAll(Arrays.asList(
                "BigDecimal", "BigInteger", "Date", "LocalDate", "LocalDateTime"));

        DtoField tempField = new DtoField(0, "", type, "", false, "", false);
        String formattedType = tempField.getFormattedDataType();

        return simpleTypes.contains(formattedType);
    }

    private void updateSimpleListDataType(DtoField field) {
        String genericType = extractGenericType(field.getDataType());
        DtoField tempField = new DtoField(0, "", genericType, "", false, "", false);
        String formattedType = tempField.getFormattedDataType();
        field.setDataType("List<" + formattedType + ">");
    }
}