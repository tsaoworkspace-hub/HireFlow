# 公開原始碼前驗收

## 本次修正

- local profile 自動載入本機 `.env`；測試 profile 不載入真實憑證。
- Linux CI 用 `bash ./mvnw` 執行 Wrapper，明確設定唯讀 contents 權限。
- 新增獨立 PostgreSQL 17 CI，驗證 Flyway 與 Hibernate Schema。
- Email／Calendar 派送使用明確的獨立交易，悲觀鎖避免多個 worker 同時處理同一工作；重新確認工作狀態，跳過已完成與尚未到期工作。
- SMTP 設定連線與讀寫逾時；只有啟用寄信時才檢查 Mail health。

## 仍需真實操作驗收

2026-11-25 已驗證：後端一般測試 48 項通過；Gmail SMTP 登入測試另外通過（未寄信）；獨立空白 PostgreSQL 17 的 migration／schema 測試通過；前端 18 項測試與 production build 通過。一般測試會跳過需手動啟用的 Gmail／PostgreSQL 測試。

1. Gmail：在整合中心確認測試通知為 SENT，並檢查指定收件匣確實收到。
2. Calendar：由公司使用者授權 Google；新增、調整及取消測試面試，確認自己的 Google Calendar 事件同步。
3. 權限：跨部門主管不能看見對方履歷／面試；面試官只能存取被指派的資料。
4. 前端：逐頁檢查求職入口、應徵時間軸、Offer 與手機排版，擷取去識別化畫面補到 README。
5. 遠端：以 GitHub Actions 真正執行結果確認 CI，不能以本機成功代替。

## 2026-11-26 補充驗收

- Gmail 實際寄送：由 opt-in 測試將一封中性通知交給 Gmail，派送資料保存為 SENT；收件匣到達仍待使用者確認。地址只由環境變數注入，不寫入原始碼。
- Email／Calendar 整合模組 Java 與前端服務完成格式整理；解除連結及重試失敗時，整合中心會顯示錯誤訊息。
- 瀏覽器操作：HR 登入、公開職缺、虛構履歷送出、求職者進度、初審轉換、面試安排與公司行事曆顯示通過。截圖使用虛構展示資料。
- 手機公開職缺頁以 390px 寬度檢查，文件寬度為 390px，未發現水平溢出；尚未逐一完成全部手機頁面及完整 Offer 瀏覽器流程。
- 舊 GitHub CI 已確認 frontend 通過、backend 以 exit code 126 失敗；本次 bash Wrapper 修正需推送後重新確認。
- 操作時發現原 8080 程序已占用，初步畫面驗收的一筆虛構應徵及面試留在原開發實例，未刪除使用者資料。獨立驗收前端改用 HIREFLOW_DEV_API_TARGET 指定後端。

## 外部派送限制

SMTP／Google HTTP 與資料庫不共用同一交易。外部服務成功後，若程序中斷或資料庫 commit 失敗，仍可能重複寄送或建立事件；本次修正不宣稱 exactly-once。正式營運前需進一步設計冪等、派送追蹤與事件對帳。

## 正式網站上線另需處理

HTTPS、正式 Cookie／網域、關閉或更換 Demo 帳密、限流與濫用防護、資料庫備份、履歷物件儲存及個資保存政策。公開 GitHub 原始碼不等於網站已可正式營運。
