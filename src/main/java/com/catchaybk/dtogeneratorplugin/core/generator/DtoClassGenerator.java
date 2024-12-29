package com.catchaybk.dtogeneratorplugin.core.generator;

import com.catchaybk.dtogeneratorplugin.core.model.Field;
import com.catchaybk.dtogeneratorplugin.core.model.UserConfig;
import com.catchaybk.dtogeneratorplugin.intellij.ui.dialog.ValidationMessageSettingDialog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public String generateClass(String className, List<Field> fields) {
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

    private void generateImports(StringBuilder sb, List<Field> fields) {
        Set<String> imports = collectImports(fields);
        imports.stream()
                .sorted()
                .forEach(imp -> sb.append("import ").append(imp).append(";\n"));
        sb.append("\n");
    }

    private Set<String> collectImports(List<Field> fields) {
        Set<String> imports = new HashSet<>();
        imports.add("com.fasterxml.jackson.annotation.JsonProperty");

        if (!config.jsonAliasStyles.isEmpty()) {
            imports.add("com.fasterxml.jackson.annotation.JsonAlias");
        }

        imports.add("lombok.Data");

        String validationPackage = config.isJava17 ? "jakarta.validation" : "javax.validation";

        for (Field field : fields) {
            // 添加字段本身需要的導入
            imports.addAll(field.getRequiredImports());

            if (field.isRequired()) {
                if (field.getDataType().toLowerCase().contains("string")) {
                    imports.add(validationPackage + ".constraints.NotBlank");
                } else {
                    imports.add(validationPackage + ".constraints.NotNull");
                }
            }

            // 對於String類型且有大小限制的字段
            if (field.getDataType().toLowerCase().contains("string") && !field.getSize().isEmpty()) {
                imports.add(validationPackage + ".constraints.Size");
            }

            // 對於decimal類型且有大小限制的字段
            if ((field.getDataType().toLowerCase().contains("decimal")) && !field.getSize().isEmpty()) {
                imports.add(validationPackage + ".constraints.Digits");
            }

            // 對於對象類型或包含對象的List
            if ((field.isObject() && !field.isList()) ||
                    (field.isList() && !field.isPrimitiveOrWrapperType(field.getDataType()))) {
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

    private void generateClassDefinition(StringBuilder sb, String className, List<Field> fields) {
        sb.append("@Data\n");
        sb.append("public class ").append(className).append(" {\n\n");
        generateFields(sb, fields);
        sb.append("}\n");
    }

    private void generateFields(StringBuilder sb, List<Field> fields) {
        for (Field field : fields) {
            generateFieldComment(sb, field);
            generateFieldAnnotations(sb, field);
            generateFieldDeclaration(sb, field);
            sb.append("\n");
        }
    }

    private void generateFieldComment(StringBuilder sb, Field field) {
        if (field.getComments() != null && !field.getComments().isEmpty()) {
            sb.append("    /** ").append(field.getComments()).append(" */\n");
        }
    }

    private void generateFieldAnnotations(StringBuilder sb, Field field) {
        if (field.isRequired()) {
            if (field.getDataType().toLowerCase().contains("string")) {
                sb.append("    @NotBlank(message = \"")
                        .append(ValidationMessageSettingDialog.getNotBlankMessage(
                                field.getCamelCaseName(), field.getComments()))
                        .append("\")\n");
            } else {
                sb.append("    @NotNull(message = \"")
                        .append(ValidationMessageSettingDialog.getNotNullMessage(
                                field.getCamelCaseName(), field.getComments()))
                        .append("\")\n");
            }
        }

        if ((field.isObject() && !field.isList()) ||
                (field.isList() && !field.isPrimitiveOrWrapperType(field.getDataType()))) {
            sb.append("    @Valid\n");
        }

        // JsonProperty 註解
        String jsonPropertyName = field.formatName(config.jsonPropertyStyle);
        sb.append("    @JsonProperty(\"").append(jsonPropertyName).append("\")\n");

        // JsonAlias 註解（如果有選擇的格式）
        List<String> aliasNames = config.jsonAliasStyles.stream()
                .map(field::formatName)
                .filter(name -> !name.equals(jsonPropertyName)) // 排除與 JsonProperty 相同的名稱
                .distinct() // 去重
                .collect(Collectors.toList());

        if (!aliasNames.isEmpty()) {
            sb.append("    @JsonAlias({")
                    .append(aliasNames.stream()
                            .map(name -> "\"" + name + "\"")
                            .collect(Collectors.joining(", ")))
                    .append("})\n");
        }

        String lowerType = field.getDataType().toLowerCase();
        // 對於 decimal 和 bigdecimal 類型且有 size 的字段
        if ((lowerType.equals("decimal") || lowerType.equals("bigdecimal"))
                && !field.getSize().isEmpty()) {
            String[] parts = field.getSize().split(",");
            String integer = parts[0];
            String fraction = parts.length > 1 ? parts[1] : "0";

            sb.append("    @Digits(integer = ").append(integer)
                    .append(", fraction = ").append(fraction)
                    .append(", message = \"")
                    .append(ValidationMessageSettingDialog.getDigitsMessage(
                            field.getCamelCaseName(), field.getComments(), field.getSize()))
                    .append("\")\n");
        } else if (field.getDataType().toLowerCase().contains("string")
                && !field.getSize().isEmpty()) {
            sb.append("    @Size(max = ").append(field.getSize())
                    .append(", message = \"")
                    .append(ValidationMessageSettingDialog.getSizeMessage(
                            field.getCamelCaseName(), field.getComments(), field.getSize()))
                    .append("\")\n");
        }
    }

    private void generateFieldDeclaration(StringBuilder sb, Field field) {
        sb.append("    private ")
                .append(field.getFormattedDataType())
                .append(" ")
                .append(field.getCamelCaseName())
                .append(";\n");
    }
}