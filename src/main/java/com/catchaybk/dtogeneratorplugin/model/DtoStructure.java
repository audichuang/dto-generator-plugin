package com.catchaybk.dtogeneratorplugin.model;

import java.util.ArrayList;
import java.util.List;

public class DtoStructure {
    private String className;
    private List<DtoField> fields;
    private List<DtoStructure> childStructures;
    private DtoField parentField;

    public DtoStructure(String className) {
        this.className = className;
        this.fields = new ArrayList<>();
        this.childStructures = new ArrayList<>();
    }

    // 用於兼容舊代碼的構造函數
    public DtoStructure(int level, String parentField, List<DtoField> fields) {
        this.className = parentField != null ? parentField + "DTO" : "MainDTO";
        this.fields = new ArrayList<>(fields);
        this.childStructures = new ArrayList<>();
    }

    public void addField(DtoField field) {
        fields.add(field);
    }

    public void addChildStructure(DtoStructure childStructure, DtoField parentField) {
        childStructures.add(childStructure);
        childStructure.setParentField(parentField);
    }

    public String getKey() {
        return (parentField != null ? parentField.getLevel() : "1") + ":" +
                (parentField != null ? parentField.getDataName() : "main");
    }

    // Getters and Setters
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<DtoField> getFields() {
        return fields;
    }

    public List<DtoStructure> getChildStructures() {
        return childStructures;
    }

    public DtoField getParentField() {
        return parentField;
    }

    public void setParentField(DtoField parentField) {
        this.parentField = parentField;
    }

    // 兼容舊代碼的方法
    public int getLevel() {
        return parentField != null ? parentField.getLevel() + 1 : 1;
    }
}