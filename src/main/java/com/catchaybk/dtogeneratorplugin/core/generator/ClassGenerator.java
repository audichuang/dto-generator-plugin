package com.catchaybk.dtogeneratorplugin.core.generator;

import com.catchaybk.dtogeneratorplugin.core.model.Field;
import com.catchaybk.dtogeneratorplugin.core.model.UserConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 類生成器
 * 負責生成DTO類的代碼內容
 */
public class ClassGenerator {
    private final String packageName;
    private final UserConfig config;

    public ClassGenerator(String packageName, UserConfig config) {
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

        // 添加驗證註解
        String validationAnnotations = field.getValidationAnnotations();
        if (!validationAnnotations.isEmpty()) {
            sb.append("    ").append(validationAnnotations).append("\n");
        }

        // 添加 JsonProperty 註解
        String jsonPropertyName = field.formatName(config.jsonPropertyStyle);
        if (jsonPropertyName != null) {
            sb.append("    @JsonProperty(\"").append(jsonPropertyName).append("\")\n");
        }

        // 添加 JsonAlias 註解
        Set<String> aliases = new HashSet<>();
        for (String style : config.jsonAliasStyles) {
            String alias = field.formatName(style);
            if (alias != null && !alias.equals(jsonPropertyName)) {
                aliases.add(alias);
            }
        }

        if (!aliases.isEmpty()) {
            sb.append("    @JsonAlias({\"")
                    .append(String.join("\", \"", aliases))
                    .append("\"})\n");
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