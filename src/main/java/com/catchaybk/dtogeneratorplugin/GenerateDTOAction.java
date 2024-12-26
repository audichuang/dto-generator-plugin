package com.catchaybk.dtogeneratorplugin;

import com.catchaybk.dtogeneratorplugin.model.DtoField;
import com.catchaybk.dtogeneratorplugin.ui.DtoGeneratorDialog;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenerateDTOAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // 獲取當前編輯器中的文件所在目錄
        PsiFile currentFile = e.getData(CommonDataKeys.PSI_FILE);
        PsiDirectory directory = currentFile != null ? currentFile.getContainingDirectory() : null;

        if (directory == null) {
            return;
        }

        // 顯示DTO生成器對話框
        DtoGeneratorDialog dialog = new DtoGeneratorDialog();
        if (dialog.showAndGet()) {
            List<DtoField> dtoFields = dialog.getDtoFields();
            generateDTOFiles(project, directory, dtoFields);
        }
    }

    private void generateDTOFiles(Project project, PsiDirectory directory, List<DtoField> fields) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                // 按level分組
                Map<Integer, List<DtoField>> fieldsByLevel = fields.stream()
                        .collect(Collectors.groupingBy(DtoField::getLevel));

                // 生成主DTO類
                if (fieldsByLevel.containsKey(1)) {
                    createJavaClass(project, directory, "MainDTO",
                            generateDtoClass("MainDTO", fieldsByLevel.get(1)));
                }

                // 生成第二層DTO類
                if (fieldsByLevel.containsKey(2)) {
                    createJavaClass(project, directory, "SupListDTO",
                            generateDtoClass("SupListDTO", fieldsByLevel.get(2)));
                }

                // 生成第三層DTO類
                if (fieldsByLevel.containsKey(3)) {
                    createJavaClass(project, directory, "SubSeqnoDTO",
                            generateDtoClass("SubSeqnoDTO", fieldsByLevel.get(3)));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private String generateDtoClass(String className, List<DtoField> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.example.dto;\n\n");

        // 添加導入
        sb.append("import com.fasterxml.jackson.annotation.JsonProperty;\n");
        sb.append("import lombok.Data;\n");
        sb.append("import java.util.List;\n\n");

        // 添加類註解
        sb.append("/**\n");
        sb.append(" * ").append(className).append("\n");
        sb.append(" */\n");

        sb.append("@Data\n");
        sb.append("public class ").append(className).append(" {\n\n");

        for (DtoField field : fields) {
            // 添加字段註解
            sb.append("    /** ").append(field.getComments()).append(" */\n");

            // 添加 JsonProperty 註解
            sb.append("    @JsonProperty(\"").append(field.getOriginalName()).append("\")\n");

            // 添加字段定義
            sb.append("    private ").append(field.getDataType())
              .append(" ").append(field.getCamelCaseName()).append(";\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private void createJavaClass(Project project, PsiDirectory directory, String className, String classContent) {
        PsiFileFactory fileFactory = PsiFileFactory.getInstance(project);
        PsiJavaFile psiFile = (PsiJavaFile) fileFactory.createFileFromText(
                className + ".java",
                StdFileTypes.JAVA,
                classContent
        );

        // 格式化代碼
        JavaCodeStyleManager.getInstance(project).optimizeImports(psiFile);
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiFile);

        // 將文件保存到目標目錄
        directory.add(psiFile);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}