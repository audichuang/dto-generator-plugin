package com.catchaybk.dtogeneratorplugin;

import com.catchaybk.dtogeneratorplugin.model.DtoField;
import java.util.List;
import java.util.Map;

public class UserConfig {
    public final List<DtoField> dtoFields;
    public final String mainClassName;
    public final String author;
    public final String msgId;
    public final boolean isJava17;
    public final String messageDirectionComment;
    public final Map<Integer, Map<String, String>> levelClassNamesMap;
    public final String targetPackage;

    public UserConfig(List<DtoField> dtoFields, String mainClassName, String author,
            String msgId, boolean isJava17, String messageDirectionComment,
            Map<Integer, Map<String, String>> levelClassNamesMap, String targetPackage) {
        this.dtoFields = dtoFields;
        this.mainClassName = mainClassName;
        this.author = author;
        this.msgId = msgId;
        this.isJava17 = isJava17;
        this.messageDirectionComment = messageDirectionComment;
        this.levelClassNamesMap = levelClassNamesMap;
        this.targetPackage = targetPackage;
    }
}