package com.catchaybk.dtogeneratorplugin.model;

import java.util.Map;

/**
 * DTO配置數據模型
 * 存儲所有DTO生成相關的配置信息
 */
public class DtoConfigData {
    private final String targetPackage;
    private final String msgId;
    private final String author;
    private final boolean isJava17;
    private final String messageDirection;
    private final Map<String, String> classNames;

    public DtoConfigData(String targetPackage, String msgId, String author,
                         boolean isJava17, String messageDirection, Map<String, String> classNames) {
        this.targetPackage = targetPackage;
        this.msgId = msgId;
        this.author = author;
        this.isJava17 = isJava17;
        this.messageDirection = messageDirection;
        this.classNames = classNames;
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
}