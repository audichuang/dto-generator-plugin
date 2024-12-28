package com.catchaybk.dtogeneratorplugin;

import com.catchaybk.dtogeneratorplugin.model.DtoField;
import com.catchaybk.dtogeneratorplugin.model.DtoStructure;
import com.catchaybk.dtogeneratorplugin.ui.DtoGeneratorDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GenerateDTOAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        DtoGeneratorDialog dialog = new DtoGeneratorDialog();
        if (dialog.showAndGet()) {
            List<DtoField> dtoFields = dialog.getDtoFields();
            String mainClassName = dialog.getMainClassName();
            String author = dialog.getAuthor();
            String msgId = dialog.getMsgId();
            boolean isJava17 = dialog.isJava17();
            String messageDirectionComment = dialog.getMessageDirectionComment();
            Map<Integer, Map<String, String>> levelClassNamesMap = dialog.getLevelClassNamesMap();

            PsiFile currentFile = e.getData(CommonDataKeys.PSI_FILE);
            PsiDirectory directory = currentFile != null ? currentFile.getContainingDirectory() : null;

            if (directory == null) return;

            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    generateDtoClasses(project, directory, dtoFields, mainClassName, author, msgId, messageDirectionComment, levelClassNamesMap, isJava17);
                } catch (Exception ex) {
                    Messages.showErrorDialog(project, "Error generating DTOs: " + ex.getMessage(), "Error");
                }
            });
        }
    }

    private void generateDtoClasses(Project project, PsiDirectory directory,
                                    List<DtoField> allFields, String mainClassName,
                                    String author, String msgId, String messageDirectionComment,
                                    Map<Integer, Map<String, String>> levelClassNamesMap,
                                    boolean isJava17) {
        DtoStructure mainStructure = analyzeDtoStructure(allFields, mainClassName, levelClassNamesMap);
        generateAllClasses(project, directory, mainStructure, author, msgId, messageDirectionComment, isJava17);
    }

    private DtoStructure analyzeDtoStructure(List<DtoField> allFields, String mainClassName,
                                             Map<Integer, Map<String, String>> levelClassNamesMap) {
        // 找出最小層級
        int minLevel = allFields.stream()
                .mapToInt(DtoField::getLevel)
                .min()
                .orElse(1); // 如果沒有字段，默認為1

        // 創建主結構
        DtoStructure mainStructure = new DtoStructure(mainClassName);
        Map<Integer, Map<String, DtoStructure>> levelStructures = new HashMap<>();
        Map<String, DtoStructure> currentLevelStructures = new HashMap<>();
        levelStructures.put(minLevel, currentLevelStructures);
        currentLevelStructures.put("main", mainStructure);

        // 按層級分組字段
        Map<Integer, List<DtoField>> levelFields = new HashMap<>();
        for (DtoField field : allFields) {
            levelFields.computeIfAbsent(field.getLevel(), k -> new ArrayList<>()).add(field);
        }

        // 處理每一層級
        Set<Integer> levels = new TreeSet<>(levelFields.keySet()); // 使用TreeSet來確保層級順序
        for (Integer level : levels) {
            List<DtoField> fields = levelFields.get(level);
            if (fields == null) continue;

            // 獲取當前層級的結構映射
            currentLevelStructures = levelStructures.computeIfAbsent(level, k -> new HashMap<>());

            for (DtoField field : fields) {
                // 找到父字段和父結構
                DtoField parentField = findParentField(allFields, field);
                DtoStructure parentStructure;
                if (level == minLevel) {
                    parentStructure = mainStructure;
                } else {
                    Map<String, DtoStructure> parentLevelStructures = levelStructures.get(level - 1);
                    String parentKey = parentField != null ? parentField.getDataName() : "main";
                    parentStructure = parentLevelStructures.get(parentKey);
                    if (parentStructure == null) continue;
                }

                // 如果是對象或列表類型，創建新的結構
                // 在 analyzeDtoStructure 方法中修改處理字段類型的部分
                if (field.isObject() || field.isList()) {
                    // 如果是列表類型，檢查是否需要創建新的結構
                    if (field.isList()) {
                        String dataType = field.getDataType().toLowerCase();
                        // 檢查是否是基本型別的列表
                        if (dataType.startsWith("list<")) {
                            String genericType = dataType.substring(5, dataType.length() - 1).trim();
                            if (field.isPrimitiveOrWrapperType(genericType)) {
                                // 如果是基本型別的列表，直接使用該類型，不創建新結構
                                field.setDataType("List<" + genericType + ">");
                                parentStructure.addField(field);
                                continue;
                            }
                        }
                    }

                    // 從配置中獲取類名
                    String configuredClassName = null;
                    Map<String, String> levelMap = levelClassNamesMap.get(level);
                    if (levelMap != null) {
                        configuredClassName = levelMap.get(field.getDataName());
                    }

                    // 如果沒有配置的類名，使用默認的
                    if (configuredClassName == null || configuredClassName.isEmpty()) {
                        if (field.getDataName().equals("SupList")) {
                            configuredClassName = levelClassNamesMap.get(1).get("SupList");
                        } else if (field.getDataName().equals("SubSeqnoList")) {
                            configuredClassName = levelClassNamesMap.get(2).get("SubSeqnoList");
                        } else {
                            configuredClassName = field.getDataName() + "DTO";
                        }
                    }

                    // 設置子類名稱
                    field.setChildClassName(configuredClassName);

                    // 更新字段的數據類型
                    if (field.isList()) {
                        field.setDataType("List<" + configuredClassName + ">");
                    } else {
                        field.setDataType(configuredClassName);
                    }

                    // 創建子結構
                    DtoStructure childStructure = new DtoStructure(configuredClassName);
                    parentStructure.addChildStructure(childStructure, field);
                    currentLevelStructures.put(field.getDataName(), childStructure);
                }

                // 添加字段到父結構
                parentStructure.addField(field);
            }
        }
        return mainStructure;
    }

    private DtoField findParentField(List<DtoField> allFields, DtoField currentField) {
        int currentIndex = allFields.indexOf(currentField);
        int targetLevel = currentField.getLevel() - 1;

        // 從當前字段向前查找最近的父層級字段
        for (int i = currentIndex - 1; i >= 0; i--) {
            DtoField field = allFields.get(i);
            if (field.getLevel() == targetLevel && (field.isObject() || field.isList())) {
                return field;
            }
            // 如果找到更低層級的字段，則停止查找
            if (field.getLevel() < targetLevel) {
                break;
            }
        }
        return null;
    }

    private void generateAllClasses(Project project, PsiDirectory directory,
                                    DtoStructure structure, String author, String msgId,
                                    String messageDirectionComment, boolean isJava17) {
        String classContent = generateDtoClass(structure.getClassName(),
                structure.getFields(), author, msgId, messageDirectionComment, isJava17);
        createOrUpdateJavaClass(project, directory, structure.getClassName(), classContent);

        for (DtoStructure childStructure : structure.getChildStructures()) {
            generateAllClasses(project, directory, childStructure, author, msgId,
                    messageDirectionComment, isJava17);
        }
    }

    private void createOrUpdateJavaClass(Project project, PsiDirectory directory,
                                         String className, String classContent) {
        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        String fileName = className + ".java";

        // 檢查文件是否已存在
        PsiFile existingFile = directory.findFile(fileName);
        if (existingFile != null) {
            existingFile.delete();
        }

        // 創建新的Java文件
        PsiFile file = factory.createFileFromText(fileName,
                StdFileTypes.JAVA, // 使用 StdFileTypes.JAVA 替代 JavaFileType
                classContent);

        // 添加到目錄
        directory.add(file);

        // 優化導入
        if (file instanceof PsiJavaFile) {
            JavaCodeStyleManager.getInstance(project).optimizeImports((PsiJavaFile) file);
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(file);
        }
    }

    private String generateDtoClass(String className, List<DtoField> fields, String author, String msgId, String messageDirectionComment, boolean isJava17) {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.example.dto;\n\n");

        // 添加導入
        Set<String> imports = new HashSet<>();
        imports.add("com.fasterxml.jackson.annotation.JsonProperty");
        imports.add("lombok.Data");

        // 根據 Java 版本添加驗證相關的導入
        if (isJava17) {
            imports.add("jakarta.validation.constraints.NotNull");
            imports.add("jakarta.validation.constraints.NotBlank");
            imports.add("jakarta.validation.constraints.Size");
        } else {
            imports.add("javax.validation.constraints.NotNull");
            imports.add("javax.validation.constraints.NotBlank");
            imports.add("javax.validation.constraints.Size");
        }

//        // 檢查是否需要List導入
//        if (fields.stream().anyMatch(f -> f.isList())) {
//            imports.add("java.util.List");
//        }
//
//        // 添加所有導入
//        List<String> sortedImports = new ArrayList<>(imports);
//        Collections.sort(sortedImports);
//        for (String imp : sortedImports) {
//            sb.append("import ").append(imp).append(";\n");
//        }
//        sb.append("\n");

        // 收集所有字段需要的導入
        for (DtoField field : fields) {
            imports.addAll(field.getRequiredImports());
        }

        // 按字母順序排序並添加導入語句
        List<String> sortedImports = new ArrayList<>(imports);
        Collections.sort(sortedImports);
        for (String imp : sortedImports) {
            sb.append("import ").append(imp).append(";\n");
        }
        sb.append("\n");

        // 添加類註解
        sb.append("/**\n");
        sb.append(" * ").append(msgId).append("\n");
        // 根據電文方向添加說明
        sb.append(" * ").append(messageDirectionComment).append("\n");
        if (author != null && !author.isEmpty()) {
            sb.append(" * @author ").append(author).append("\n");
        }
        sb.append(" */\n");

        sb.append("@Data\n");
        sb.append("public class ").append(className).append(" {\n\n");

        // 添加字段
        for (DtoField field : fields) {
            // 添加字段註釋
            if (field.getComments() != null && !field.getComments().isEmpty()) {
                sb.append("    /** ").append(field.getComments()).append(" */\n");
            }

            // 添加驗證註解
            if (!field.isNullable()) {
                if (field.getDataType().toLowerCase().equals("string")) {
                    sb.append("    @NotBlank\n");
                } else {
                    sb.append("    @NotNull\n");
                }
            }
            // 添加大小限制註解
            if (field.getSize() != null && !field.getSize().isEmpty()) {
                try {
                    int size = Integer.parseInt(field.getSize());
                    if (field.getDataType().toLowerCase().equals("string")) {
                        sb.append("    @Size(max = ").append(size).append(")\n");
                    }
                } catch (NumberFormatException ignored) {
                    // 如果size不是有效的數字，則忽略
                }
            }

            // 添加 JsonProperty 註解
            sb.append("    @JsonProperty(\"").append(field.getOriginalName()).append("\")\n");

            // 添加字段定義
            sb.append("    private ").append(field.getFormattedDataType())
                    .append(" ").append(field.getCamelCaseName()).append(";\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}
// TODO 如果用受信超大包 他config dialog包判斷 會不準確不會顯示所有包 要修正 應該是小寫list的問題
