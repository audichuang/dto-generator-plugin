package com.catchaybk.dtogeneratorplugin.core.model;

import java.util.Map;

public class DtoConfigData {
    private final String targetPackage;
    private final String msgId;
    private final String author;
    private final boolean isJava17;
    private final String messageDirection;
    private final Map<String, String> classNames;
    private final String mainClassName;

    public DtoConfigData(String targetPackage, String msgId, String author,
            boolean isJava17, String messageDirection, Map<String, String> classNames) {
        this.targetPackage = targetPackage;
        this.msgId = msgId;
        this.author = author;
        this.isJava17 = isJava17;
        this.messageDirection = messageDirection;
        this.classNames = classNames;
        this.mainClassName = classNames.get("main");
    }

    public String getTargetPackage() {
        return targetPackage;
    }

    public String getMsgId() {
        return msgId;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isJava17() {
        return isJava17;
    }

    public String getMessageDirection() {
        return messageDirection;
    }

    public Map<String, String> getClassNames() {
        return classNames;
    }

    public String getMainClassName() {
        return mainClassName;
    }
}