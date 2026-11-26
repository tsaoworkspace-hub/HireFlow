# 公開原始碼前驗收

## 本次修正

- local profile 自動載入本機 `.env`；測試 profile 不載入真實憑證。
- Linux CI 用 `bash ./mvnw` 執行 Wrapper，明確設定唯讀 contents 權限。
- 新增獨立 PostgreSQL 17 CI，驗證 Flyway 與 Hibernate Schema。
- Email／Calendar 派送使用明確的獨立交易，悲觀鎖避免多個 worker 同時處理同一工作；重新確認工作狀態，跳過已完成與尚未到期工作。
- SMTP 設定連線與讀寫逾時；只有啟用寄信時才檢查 Mail health。

## 驗收基準與追蹤

2026-11-25 已驗證：後端一般測試 48 項通過；Gmail SMTP 登入測試另外通過（未寄信）；獨立空白 PostgreSQL 17 的 migration／schema 測試通過；前端 18 項測試與 production build 通過。一般測試會跳過需手動啟用的 Gmail／PostgreSQL 測試。

1. Gmail：在整合中心確認測試通知為 SENT，並檢查指定收件匣確實收到。
2. Calendar：真實授權、事件新增／取消與改期的同步工作已通過，詳見下方補充紀錄。
3. 權限：跨部門應徵資料及非指派面試的拒絕存取已有整合測試；瀏覽器另驗證面試官不顯示公司管理操作。
4. 前端：公開職缺、求職者進度、Offer 完整流程及主要手機版面已驗證；截圖只含虛構展示資料。
5. 遠端：commit `ee58d24` 的 GitHub Actions backend、frontend、postgres 三個工作已全部通過。本次改期提交仍需確認新一輪 CI。

## 2026-11-26 補充驗收

- Gmail 實際寄送：由 opt-in 測試將一封中性通知交給 Gmail，派送資料保存為 SENT；收件匣到達仍待使用者確認。地址只由環境變數注入，不寫入原始碼。
- Email／Calendar 整合模組 Java 與前端服務完成格式整理；解除連結及重試失敗時，整合中心會顯示錯誤訊息。
- 瀏覽器操作：HR 登入、公開職缺、虛構履歷送出、求職者進度、初審轉換、面試安排與公司行事曆顯示通過。截圖使用虛構展示資料。
- 手機公開職缺頁以 390px 寬度檢查，未發現水平溢出；公司端面試、行事曆及 Offer 詳細頁亦完成 390px 檢查。這是版面抽查，並非所有裝置與瀏覽器認證。
- 舊 GitHub CI 的 exit code 126 及 Wrapper distributionUrl 問題均已修正，三個工作已在後續執行通過。
- 操作時發現原 8080 程序已占用，初步畫面驗收的一筆虛構應徵及面試留在原開發實例，未刪除使用者資料。獨立驗收前端改用 HIREFLOW_DEV_API_TARGET 指定後端。
- Google Calendar 真實授權完成後，虛構面試 #1 的 UPSERT 工作完成；再透過應用程式取消該筆面試，DELETE 工作亦完成。面試目前為 CANCELLED；Google 帳號連結保留。截圖僅包含 Demo 身分與虛構測試資料。
- 遠端 CI 的第二個問題是 Wrapper properties 最後一行缺少換行，POSIX shell 因而漏讀 distributionUrl。已補上行結尾、`.gitattributes` 與 executable bit，修正後三個工作全數通過。

## 本次改期與 Offer 收尾

- 新增 `PUT /api/interviews/{id}/schedule` 及 HR／ADMIN 改期表單，保留原應徵、輪次與面試官；檢查未來時間、撞期、評分狀態及樂觀鎖版本，原因寫入 Audit Log。
- Calendar 工作去重鍵包含面試版本，避免改期被舊工作吞掉；已取消面試的延遲 UPSERT 直接跳過，避免重建已取消事件。
- 虛構面試 #2 從 2026-12-01 10:00–11:00 改為 14:00–15:00（Asia/Taipei），公司畫面更新成功，建立與改期的兩筆 UPSERT 均為 COMPLETED。模擬 API 測試另驗證既有事件使用 update，未宣稱外部派送 exactly-once。
- 瀏覽器依序驗證面試官送出並鎖定評分、HR 完成面試與送決策、主管錄取決策、HR 建立 Offer、主管核准、HR 寄送、求職者接受及公司端歷程。測試 Offer 明確註記「虛構、無聘僱效力」。
- Offer 通知工作顯示 SENT，收件者是保留的 `.test` 測試地址，不能視為真實信箱投遞成功；先前寄給使用者指定信箱的通知仍待使用者確認收件。
- 本次後端 53 項一般測試通過（3 項外部測試預設跳過），前端 19 項測試與 production build 通過。
- 截圖：`interview-reschedule.jpg`、`calendar-reschedule-sync.jpg`、`offer-accepted.jpg`、`mobile-offer.jpg`。私人學習筆記保存在 Repository 外，不會提交。
- 公開前掃描：當前追蹤檔與既有 8 個 commit 未找到本機 Google Client Secret、Token 加密金鑰或 Gmail App Password；`.env` 與私人筆記未被追蹤。資料庫預設密碼只是本機 Demo 預設值，正式部署必須更換。
- `npm audit --omit=dev --audit-level=high` 回報 0 vulnerabilities，範圍限前端正式依賴；不代表已完成全面滲透測試或後端依賴漏洞稽核。

## 外部派送限制

SMTP／Google HTTP 與資料庫不共用同一交易。外部服務成功後，若程序中斷或資料庫 commit 失敗，仍可能重複寄送或建立事件；本次修正不宣稱 exactly-once。正式營運前需進一步設計冪等、派送追蹤與事件對帳。

## 正式網站上線另需處理

HTTPS、正式 Cookie／網域、關閉或更換 Demo 帳密、限流與濫用防護、資料庫備份、履歷物件儲存及個資保存政策。公開 GitHub 原始碼不等於網站已可正式營運。
