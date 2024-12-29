package com.catchaybk.dtogeneratorplugin.core.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * DTO配置數據類
 * 存儲所有配置相關的數據，包括包路徑、作者、類名等信息
 */
@Getter
@RequiredArgsConstructor
public class ConfigData {
    /** 目標包路徑，生成的DTO類將放在此包下 */
    private final String targetPackage;

    /** 消息ID，用於生成類名前綴 */
    private final String msgId;

    /** 作者名稱，將添加到類的文檔註解中 */
    private final String author;

    /** 是否使用Java 17，決定使用jakarta還是javax的驗證包 */
    private final boolean isJava17;

    /** 電文方向（上行/下行/無），用於生成類名後綴 */
    private final String messageDirection;

    /** 類名映射表，key為類型名，value為對應的類名 */
    private final Map<String, String> classNames;

    /** 主類名稱，從classNames中獲取 */
    private final String mainClassName;

    /**
     * 創建配置數據實例
     *
     * @param targetPackage    目標包路徑
     * @param msgId            消息ID
     * @param author           作者名稱
     * @param isJava17         是否使用Java 17
     * @param messageDirection 電文方向
     * @param classNames       類名映射表
     */
    public ConfigData(String targetPackage, String msgId, String author,
            boolean isJava17, String messageDirection, Map<String, String> classNames) {
        this.targetPackage = targetPackage;
        this.msgId = msgId;
        this.author = author;
        this.isJava17 = isJava17;
        this.messageDirection = messageDirection;
        this.classNames = classNames;
        this.mainClassName = classNames.get("main");
    }
}