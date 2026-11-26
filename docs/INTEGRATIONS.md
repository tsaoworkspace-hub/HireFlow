# 面試協調、Email 與 Google Calendar

## 面試時段與公司行事曆

- HR 可針對 Application 建立多個候選面試時段。
- 求職者在原本的安全進度頁選擇一個時段；確認後才建立正式 Interview。
- 正式建立前再次檢查所有面試官是否撞期。
- 其他未選時段自動改為 `WITHDRAWN`。
- HR／ADMIN 可查看全部面試；主管只看自己部門；純面試官只看指派給自己的行程。

## Email Outbox 與 Gmail

核心交易只建立 `email_deliveries` 工作，不直接等待 Gmail。背景 Dispatcher 負責寄送，失敗後以 1 分鐘、5 分鐘間隔重試，三次後標示 FAILED，HR 可在整合中心人工重試。

本機 `.env`：

```env
APP_MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-account@gmail.com
MAIL_PASSWORD=your-google-app-password
MAIL_FROM=your-account@gmail.com
```

Gmail 帳號應使用 App Password，不要把一般登入密碼或 App Password 提交 Git。

預設 local profile 會自動載入根目錄 `.env`（從根目錄或 backend/ 啟動皆可）。填寫 `KEY=value`，不加引號，應用程式密碼移除空格。IDE 環境變數優先，若留有舊密碼需移除或更新；test profile 不載入本機 `.env`。

派送程式改用 `TransactionTemplate` 明確建立每筆獨立交易，避免同類別呼叫繞過 `@Transactional` 代理。工作先取得資料列鎖並重新確認狀態，成功／失敗結果在交易結束時提交；已完成或尚未到重試時間的工作直接跳過。外部 SMTP／Google 呼叫不屬於資料庫交易，程序中斷時仍需考慮重複派送，詳見 `PUBLIC_RELEASE_CHECKLIST.md`。

## Google Calendar

1. 在 Google Cloud 建立 OAuth 2.0 Web Application。
2. 啟用 Google Calendar API。
3. Authorized redirect URI 設為：

```text
http://localhost:8080/api/integrations/google-calendar/callback
```

4. 產生 32-byte AES 金鑰並用 Base64 表示，例如 PowerShell：

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

5. 設定 `.env`：

```env
GOOGLE_CALENDAR_ENABLED=true
GOOGLE_CALENDAR_CLIENT_ID=...
GOOGLE_CALENDAR_CLIENT_SECRET=...
GOOGLE_CALENDAR_REDIRECT_URI=http://localhost:8080/api/integrations/google-calendar/callback
GOOGLE_CALENDAR_TOKEN_KEY=產生的Base64金鑰
```

Access Token 與 Refresh Token 使用 AES-256-GCM 加密後才進資料庫。授權 scope 僅使用 `calendar.events`。確認面試後，只替已連結且實際參與該面試的 HR／面試官建立個人主要行事曆事件。

Google 官方參考：

- https://developers.google.com/identity/protocols/oauth2/web-server
- https://developers.google.com/workspace/calendar/api/v3/reference/events/insert

## 整合管理與失敗隔離

- `/integrations`：個人連結／解除 Google Calendar。
- HR／ADMIN 可查看 Email Delivery 與 Calendar Sync Job。
- 外部服務失敗不回滾 Application、Interview 或求職者選時段。
- Outbox Job 保存狀態、重試次數、下次重試時間與安全化錯誤摘要。
- Email 成功後清除資料庫中的內文，縮短專屬查詢連結以明文存在的時間。

## 新增入口

- 公司行事曆：`http://localhost:5173/calendar`
- 整合中心：`http://localhost:5173/integrations`
- 求職者仍使用：`http://localhost:5173/candidate/applications/{publicId}?token=...`

## 資料庫版本

- V7：`interview_time_slots` 與面試官關聯。
- V8：`email_deliveries` Outbox。
- V9：Google Calendar Connection、Event Link、Sync Job。
