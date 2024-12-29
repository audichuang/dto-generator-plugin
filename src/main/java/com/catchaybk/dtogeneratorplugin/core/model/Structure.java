package com.catchaybk.dtogeneratorplugin.core.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Structure {
    private String className;
    private List<Field> fields;
    private List<Structure> childStructures;
    private Field parentField;

    public Structure(String className) {
        this.className = className;
        this.fields = new ArrayList<>();
        this.childStructures = new ArrayList<>();
    }

    // 用於兼容舊代碼的構造函數
    public Structure(int level, String parentField, List<Field> fields) {
        this.className = parentField != null ? parentField + "DTO" : "MainDTO";
        this.fields = new ArrayList<>(fields);
        this.childStructures = new ArrayList<>();
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public void addChildStructure(Structure childStructure, Field parentField) {
        childStructures.add(childStructure);
        childStructure.setParentField(parentField);
    }

    public String getKey() {
        return (parentField != null ? parentField.getLevel() : "1") + ":" +
                (parentField != null ? parentField.getDataName() : "main");
    }

    // 兼容舊代碼的方法
    public int getLevel() {
        return parentField != null ? parentField.getLevel() + 1 : 1;
    }
}