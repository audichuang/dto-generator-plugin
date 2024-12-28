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
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GenerateDTOAction extends AnAction {
    private String targetPackage;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null)
            return;

        // 顯示配置對話框
        DtoGeneratorDialog dialog = new DtoGeneratorDialog(project);
        if (!dialog.showAndGet())
            return;

        // 獲取用戶配置
        UserConfig config = getUserConfig(dialog);

        // 創建目標目錄
        PsiDirectory targetDirectory = createPackageDirectories(project, e.getData(CommonDataKeys.PSI_FILE),
                config.targetPackage);
        if (targetDirectory == null) {
            Messages.showErrorDialog(project, "無法創建目標包路徑", "錯誤");
            return;
        }

        // 生成 DTO 類
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                generateDtoClasses(project, targetDirectory, config);
            } catch (Exception ex) {
                Messages.showErrorDialog(project, "生成DTO時發生錯誤: " + ex.getMessage(), "錯誤");
            }
        });
    }

    private UserConfig getUserConfig(DtoGeneratorDialog dialog) {
        return new UserConfig(
                dialog.getDtoFields(),
                dialog.getMainClassName(),
                dialog.getAuthor(),
                dialog.getMsgId(),
                dialog.isJava17(),
                dialog.getMessageDirectionComment(),
                dialog.getLevelClassNamesMap(),
                dialog.getTargetPackage());
    }

    private PsiDirectory createPackageDirectories(Project project, PsiFile currentFile, String packageName) {
        try {
            PsiManager psiManager = PsiManager.getInstance(project);
            VirtualFile sourceRoot = findSourceRoot(project, currentFile);
            if (sourceRoot == null)
                return null;

            PsiDirectory directory = psiManager.findDirectory(sourceRoot);
            if (directory == null)
                return null;

            this.targetPackage = packageName;
            return createDirectories(directory, packageName);
        } catch (Exception ex) {
            return null;
        }
    }

    private VirtualFile findSourceRoot(Project project, PsiFile currentFile) {
        if (currentFile != null) {
            VirtualFile sourceRoot = ProjectRootManager.getInstance(project)
                    .getFileIndex()
                    .getSourceRootForFile(currentFile.getVirtualFile());
            if (sourceRoot != null)
                return sourceRoot;
        }
        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentSourceRoots();
        return roots.length > 0 ? roots[0] : null;
    }

    private PsiDirectory createDirectories(PsiDirectory root, String packagePath) {
        PsiDirectory current = root;
        for (String dir : packagePath.split("\\.")) {
            PsiDirectory subDir = current.findSubdirectory(dir);
            current = subDir != null ? subDir : current.createSubdirectory(dir);
        }
        return current;
    }

    private void generateDtoClasses(Project project, PsiDirectory directory, UserConfig config) {
        DtoStructure mainStructure = new DtoStructureAnalyzer(
                config.dtoFields,
                config.mainClassName,
                config.levelClassNamesMap).analyze();

        generateAllClasses(project, directory, mainStructure, config);
    }

    private void generateAllClasses(Project project, PsiDirectory directory,
            DtoStructure structure, UserConfig config) {
        String classContent = new DtoClassGenerator(targetPackage, config)
                .generateClass(structure.getClassName(), structure.getFields());

        createJavaClass(project, directory, structure.getClassName(), classContent);

        // 遞歸生成子類
        for (DtoStructure childStructure : structure.getChildStructures()) {
            generateAllClasses(project, directory, childStructure, config);
        }
    }

    private void createJavaClass(Project project, PsiDirectory directory,
            String className, String classContent) {
        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        String fileName = className + ".java";

        // 刪除已存在的文件
        PsiFile existingFile = directory.findFile(fileName);
        if (existingFile != null) {
            existingFile.delete();
        }

        // 創建新文件
        PsiFile file = factory.createFileFromText(fileName, StdFileTypes.JAVA, classContent);
        directory.add(file);

        // 優化導入
        if (file instanceof PsiJavaFile) {
            JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
            styleManager.optimizeImports((PsiJavaFile) file);
            styleManager.shortenClassReferences(file);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
}
