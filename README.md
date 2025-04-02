# DTO Generator Pro

[![Version](https://img.shields.io/badge/version-1.2.0-blue.svg)](https://plugins.jetbrains.com/plugin/com.catchaybk.dto-generator-plugin)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

IntelliJ IDEA 插件，專為金融電文開發設計的 DTO 生成工具。支援從結構化數據快速生成符合規範的 DTO 類，包含完整的驗證註解和
JSON 轉換功能。

## 功能特點

### 核心功能

- 🚀 一鍵生成完整的 DTO 類結構
- 📝 支援多層級 DTO 自動生成
- ✨ 智能類型識別和轉換
- 🔄 自動處理類之間的關聯關係

### 驗證支援

- ✅ @NotNull/@NotBlank 必填驗證
- 📏 @Size 長度驗證
- 🔢 @Digits 數字格式驗證
- 🎯 @Pattern 正則表達式驗證
- 💬 支援自定義驗證消息模板

### JSON 處理

- 🔄 自動生成 @JsonProperty 註解
- 🎨 支援多種命名風格轉換
  - 原始格式
  - 全大寫
  - 全小寫
  - 大寫底線
  - 駝峰命名
- 📋 可配置 @JsonAlias 別名

## 系統要求

- IntelliJ IDEA 2024.1 或更高版本
- Java 8 或更高版本
- Lombok 插件（推薦安裝）

## 安裝方法

1. 下載插件

   - 前往 [Releases](https://github.com/audichuang/dto-generator-plugin/releases) 頁面
   - 下載最新版本的 `DTO-Generator-Pro.jar` 檔案

2. 在 IntelliJ IDEA 中安裝

   - 打開 IntelliJ IDEA
   - 進入 `Settings/Preferences → Plugins`
   - 點擊齒輪圖示 ⚙️，選擇 `Install Plugin from Disk...`
   - 選擇剛才下載的 `DTO-Generator-Pro.jar` 檔案
   - 點擊 `OK` 並重新啟動 IDE

3. 完成安裝後，重新啟動 IntelliJ IDEA 即可使用插件功能。

## 使用說明

### 基本使用流程

1. 在專案中右鍵點擊
2. 選擇 `Generate → Generate DTO`
3. 在彈出的視窗中：
   - 直接貼上 Excel/CSV 數據
   - 或手動添加字段
4. 配置生成選項
5. 點擊確定生成 DTO 類

### 數據格式要求

支援以下格式的表格數據：

| 欄位     | 說明              | 必填 | 範例         |
| -------- | ----------------- | ---- | ------------ |
| 層級     | DTO 的層級（1-3） | 是   | 1            |
| 欄位名稱 | 變數名稱          | 是   | userName     |
| 資料類型 | Java 資料類型     | 是   | String       |
| 長度     | 欄位長度限制      | 否   | 50           |
| 必填     | 是否必填（Y/N）   | 否   | Y            |
| 說明     | 欄位說明          | 否   | 使用者名稱   |
| 正則     | 驗證表達式        | 否   | [A-Za-z0-9]+ |

### 配置選項說明

#### 基本配置

- **目標包路徑**：生成的類所在包路徑
- **作者信息**：生成的類文檔中的作者信息
- **Java 版本**：選擇 Java 8 或 Java 17（影響驗證包的選擇）
- **電文方向**：影響類名生成規則
  - 無：使用原始名稱
  - 上行：添加 Tranrq 後綴
  - 下行：添加 Tranrs 後綴

#### JSON 配置

- **Property 格式**：選擇 JSON 屬性的命名風格
- **Alias 設置**：配置額外的 JSON 屬性別名

#### 驗證消息配置

- 支援自定義各種驗證註解的錯誤消息模板
- 支援變量替換：${name}、${max}、${pattern} 等

## 生成的代碼示例

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用戶資料 DTO
 * @author YourName
 */
@Data
public class UserDTO {

    @NotBlank(message = "使用者名稱不能為空")
    @Size(max = 50, message = "使用者名稱長度不得超過50")
    @Pattern(regexp = "[A-Za-z0-9]+", message = "使用者名稱只能包含字母和數字")
    @JsonProperty("UserName")
    private String userName;
// ... 其他字段
}
```

## 常見問題

**Q: 如何處理多層級 DTO？**  
A: 在數據輸入時指定不同的層級（1-3），插件會自動處理層級關係並生成相應的類結構。

**Q: 支援哪些數據類型？**  
A: 支援所有 Java 基本類型、包裝類型，以及常用類型如 String、Date、BigDecimal 等。也支援自定義類型和泛型（如 List<String>）。

**Q: 如何自定義驗證消息？**  
A: 點擊 Setting 按鈕，可以自定義各種驗證註解的錯誤消息模板。

## UI 更新日誌

### 2024 年更新 - UI 現代化改進

- **整體視覺效果**

  - 更新了配色方案，使用更柔和、現代的顏色
  - 統一了邊框和間距樣式，提升整體美觀度
  - 增加了元素間的間距，使界面更加通透清晰
  - 調整了字體大小和權重，增強層次感

- **輸入控件**

  - 增加了輸入框的高度和內邊距，提高可讀性
  - 添加了輸入框焦點效果，提升交互體驗
  - 優化了下拉框和列表框的視覺設計

- **布局優化**
  - 重新排列了頭部面板結構，使說明更加直觀
  - 優化了標題和分隔區域的設計，增強區塊感
  - 調整了滾動面板的大小和滾動速度
- **按鈕改進**
  - 優化了按鈕樣式，使其更符合現代設計趨勢
  - 調整了按鈕大小和間距，提高可點擊性
  - 統一了提示文本，使用戶更容易理解操作

這些改進保持了插件的功能不變，但大幅提升了視覺體驗和用戶友好性，使界面更符合現代 IntelliJ IDEA 的設計風格。

## 版本歷史

### v1.2.1 (2025-01-01)

- ✨ 新增 註解支援多行
- 🎨 修正 大小駝峰轉換錯誤問題

### v1.2.0 (2024-12-29)

- ✨ 新增 Pattern 正則表達式驗證
- 🎨 新增驗證消息自定義配置
- 🔧 優化配置界面
- 🐛 修復已知問題

### v1.0.0 (2024-12-27)

- 🎉 首次發布
- ✨ 基本 DTO 生成功能
- 🔄 JSON 轉換支援
- 📝 驗證註解支援

## 貢獻指南

歡迎提交 Issue 和 Pull Request！

## 授權協議

本專案採用 MIT 授權協議 - 詳見 [LICENSE](LICENSE) 文件
