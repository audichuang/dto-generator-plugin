package com.catchaybk.dtogeneratorplugin;

import com.catchaybk.dtogeneratorplugin.model.DtoField;

import java.util.*;

/**
 * DTO類生成器
 * 負責生成DTO類的代碼內容
 */
public class DtoClassGenerator {
    private final String packageName;
    private final UserConfig config;

    public DtoClassGenerator(String packageName, UserConfig config) {
        this.packageName = packageName;
        this.config = config;
    }

    public String generateClass(String className, List<DtoField> fields) {
        StringBuilder sb = new StringBuilder();
        generatePackageDeclaration(sb);
        generateImports(sb, fields);
        generateClassComment(sb);
        generateClassDefinition(sb, className, fields);
        return sb.toString();
    }

    private void generatePackageDeclaration(StringBuilder sb) {
        if (packageName != null && !packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }
    }

    private void generateImports(StringBuilder sb, List<DtoField> fields) {
        Set<String> imports = collectImports(fields);
        imports.stream()
                .sorted()
                .forEach(imp -> sb.append("import ").append(imp).append(";\n"));
        sb.append("\n");
    }

    private Set<String> collectImports(List<DtoField> fields) {
        Set<String> imports = new HashSet<>();
        imports.add("com.fasterxml.jackson.annotation.JsonProperty");
        imports.add("lombok.Data");

        String validationPackage = config.isJava17 ? "jakarta.validation" : "javax.validation";
        boolean hasValidation = false;

        for (DtoField field : fields) {
            // 添加字段本身需要的導入
            imports.addAll(field.getRequiredImports());

            // 添加驗證相關的導入
            if (field.isRequired()) {
                hasValidation = true;
                imports.add(validationPackage + ".constraints.NotNull");
                imports.add(validationPackage + ".constraints.NotBlank");
                imports.add(validationPackage + ".constraints.Size");
            }

            if ((field.isObject() && !field.isList()) || (field.isList() && field.isObject())) {
                imports.add(validationPackage + ".Valid");
            }
        }

        return imports;
    }

    private void generateClassComment(StringBuilder sb) {
        if (config.messageDirectionComment != null && !config.messageDirectionComment.isEmpty()) {
            sb.append("/**\n");
            sb.append(" * ").append(config.msgId).append("\n");
            sb.append(" * ").append(config.messageDirectionComment).append("\n");
            if (config.author != null && !config.author.isEmpty()) {
                sb.append(" * @author ").append(config.author).append("\n");
            }
            sb.append(" */\n");
        }
    }

    private void generateClassDefinition(StringBuilder sb, String className, List<DtoField> fields) {
        sb.append("@Data\n");
        sb.append("public class ").append(className).append(" {\n\n");
        generateFields(sb, fields);
        sb.append("}\n");
    }

    private void generateFields(StringBuilder sb, List<DtoField> fields) {
        for (DtoField field : fields) {
            generateFieldComment(sb, field);
            generateFieldAnnotations(sb, field);
            generateFieldDeclaration(sb, field);
            sb.append("\n");
        }
    }

    private void generateFieldComment(StringBuilder sb, DtoField field) {
        if (field.getComments() != null && !field.getComments().isEmpty()) {
            sb.append("    /** ").append(field.getComments()).append(" */\n");
        }
    }

    private void generateFieldAnnotations(StringBuilder sb, DtoField field) {
        if (field.isRequired()) {
            sb.append("    @")
                    .append(field.getDataType().toLowerCase().contains("string") ? "NotBlank" : "NotNull")
                    .append("\n");
        }

        if ((field.isObject() && !field.isList()) || (field.isList() && field.isObject())) {
            sb.append("    @Valid\n");
        }

        sb.append("    @JsonProperty(\"").append(field.getOriginalName()).append("\")\n");

        if (field.getDataType().toLowerCase().contains("string") && !field.getSize().isEmpty()) {
            sb.append("    @Size(max = ").append(field.getSize()).append(")\n");
        }
    }

    private void generateFieldDeclaration(StringBuilder sb, DtoField field) {
        sb.append("    private ")
                .append(field.getFormattedDataType())
                .append(" ")
                .append(field.getCamelCaseName())
                .append(";\n");
    }
}