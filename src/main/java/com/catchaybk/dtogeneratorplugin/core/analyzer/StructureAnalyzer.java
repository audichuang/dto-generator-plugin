package com.catchaybk.dtogeneratorplugin.core.analyzer;

import com.catchaybk.dtogeneratorplugin.core.model.Field;
import com.catchaybk.dtogeneratorplugin.core.model.Structure;

import java.util.*;

/**
 * DTO結構分析器
 * 負責分析DTO字段之間的層級關係並建立結構樹
 */
public class StructureAnalyzer {
    private final List<Field> allFields;
    private final String mainClassName;
    private final Map<Integer, Map<String, String>> levelClassNamesMap;

    public StructureAnalyzer(List<Field> allFields, String mainClassName,
                             Map<Integer, Map<String, String>> levelClassNamesMap) {
        this.allFields = allFields;
        this.mainClassName = mainClassName;
        this.levelClassNamesMap = levelClassNamesMap;
    }

    public Structure analyze() {
        int minLevel = findMinLevel();
        Structure mainStructure = new Structure(mainClassName);
        Map<Integer, Map<String, Structure>> levelStructures = initializeLevelStructures(minLevel, mainStructure);
        processFieldsByLevel(levelStructures, minLevel);
        return mainStructure;
    }

    private int findMinLevel() {
        return allFields.stream()
                .mapToInt(Field::getLevel)
                .min()
                .orElse(1);
    }

    private Map<Integer, Map<String, Structure>> initializeLevelStructures(int minLevel,
                                                                           Structure mainStructure) {
        Map<Integer, Map<String, Structure>> levelStructures = new HashMap<>();
        Map<String, Structure> currentLevelStructures = new HashMap<>();
        levelStructures.put(minLevel, currentLevelStructures);
        currentLevelStructures.put("main", mainStructure);
        return levelStructures;
    }

    private void processFieldsByLevel(Map<Integer, Map<String, Structure>> levelStructures, int minLevel) {
        Map<Integer, List<Field>> levelFields = groupFieldsByLevel();
        Set<Integer> levels = new TreeSet<>(levelFields.keySet());

        for (Integer level : levels) {
            processLevelFields(level, levelFields.get(level), levelStructures, minLevel);
        }
    }

    private Map<Integer, List<Field>> groupFieldsByLevel() {
        Map<Integer, List<Field>> levelFields = new HashMap<>();
        for (Field field : allFields) {
            levelFields.computeIfAbsent(field.getLevel(), k -> new ArrayList<>()).add(field);
        }
        return levelFields;
    }

    private void processLevelFields(Integer level, List<Field> fields,
                                    Map<Integer, Map<String, Structure>> levelStructures, int minLevel) {
        if (fields == null)
            return;

        Map<String, Structure> currentLevelStructures = levelStructures.computeIfAbsent(level, k -> new HashMap<>());

        for (Field field : fields) {
            Structure parentStructure = findParentStructure(field, level, minLevel, levelStructures);
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

    private Structure findParentStructure(Field field, Integer level, int minLevel,
                                          Map<Integer, Map<String, Structure>> levelStructures) {
        if (level == minLevel) {
            return levelStructures.get(minLevel).get("main");
        }

        Field parentField = findParentField(field);
        if (parentField == null)
            return null;

        Map<String, Structure> parentLevelStructures = levelStructures.get(level - 1);
        return parentLevelStructures.get(parentField.getDataName());
    }

    private Field findParentField(Field currentField) {
        int currentIndex = allFields.indexOf(currentField);
        int targetLevel = currentField.getLevel() - 1;

        for (int i = currentIndex - 1; i >= 0; i--) {
            Field field = allFields.get(i);
            if (field.getLevel() == targetLevel && (field.isObject() || field.isList())) {
                return field;
            }
            if (field.getLevel() < targetLevel) {
                break;
            }
        }
        return null;
    }

    private void processComplexField(Field field, Integer level,
                                     Structure parentStructure, Map<String, Structure> currentLevelStructures) {
        String className = determineClassName(field, level);
        field.setChildClassName(className);
        updateFieldDataType(field);

        Structure childStructure = new Structure(className);
        parentStructure.addChildStructure(childStructure, field);
        currentLevelStructures.put(field.getDataName(), childStructure);
    }

    private String determineClassName(Field field, Integer level) {
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

    private void updateFieldDataType(Field field) {
        if (field.isList()) {
            field.setDataType("List<" + field.getChildClassName() + ">");
        } else {
            field.setDataType(field.getChildClassName());
        }
    }

    private boolean shouldCreateNewStructure(Field field) {
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

        Field tempField = new Field(0, "", type, "", false, "", false);
        String formattedType = tempField.getFormattedDataType();

        return simpleTypes.contains(formattedType);
    }

    private void updateSimpleListDataType(Field field) {
        String genericType = extractGenericType(field.getDataType());
        Field tempField = new Field(0, "", genericType, "", false, "", false);
        String formattedType = tempField.getFormattedDataType();
        field.setDataType("List<" + formattedType + ">");
    }
}