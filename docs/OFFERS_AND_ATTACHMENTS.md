# Offer 結案與 PDF 履歷附件

Offer 將「錄取決策」延伸成可結案的流程，並讓求職者在安全入口更新 PDF 履歷。

## Offer 流程

```text
已錄取 Application
  → HR 建立 DRAFT
  → HR 送審 PENDING_APPROVAL
  → 該部門主管 APPROVED 或 RETURNED
  → HR 寄出 SENT
  → 求職者 ACCEPTED 或 DECLINED
```

- 只有具備 `HIRED` 決策的 Application 能建立一份 Offer。
- HR／ADMIN 可建立、修改、送審、寄出與取消。
- 用人主管只能查看自己部門，且只能核准或退回自己部門的 Offer。
- 寄出時不直接呼叫 Gmail；先建立 `email_deliveries` Outbox 工作。
- Email 內含新建立的 Candidate Portal magic link，求職者不需建立帳號。
- 到期排程會把未回覆 Offer 改為 `EXPIRED`，並在到期前 24 小時建立一次提醒 Email。
- 每次狀態異動都寫入 `job_offer_histories`；重要動作也寫入全域 Audit Log。

## PDF 履歷附件

- 求職者由 Candidate Portal 上傳，僅接受檔頭為 `%PDF-` 的 PDF。
- 預設上限 5 MB，可用 `RESUME_MAX_BYTES` 調整。
- 檔案使用系統產生的 UUID 名稱，原始檔名只保存為顯示資料。
- 儲存路徑先正規化並驗證仍位於指定資料夾內，避免 Path Traversal。
- 新版本上傳後，舊版本資料保留但標示 inactive；公司端只顯示最新版。
- HR／ADMIN 可查看全部，用人主管只可下載自己部門的履歷。
- 本機檔案預設放在 `backend/data/resumes`（依啟動工作目錄而定），`data/` 已排除於 Git。

正式多機部署時，本機磁碟應替換成 S3、Azure Blob Storage 等物件儲存；資料表只保存 storage key 與中繼資料。

## API

公司端：

- `GET /api/offers`
- `GET /api/offers/options`
- `POST /api/offers`
- `PUT /api/offers/{id}`
- `POST /api/offers/{id}/submit`
- `POST /api/offers/{id}/review`
- `POST /api/offers/{id}/send`
- `POST /api/offers/{id}/cancel`
- `GET /api/applications/{applicationId}/attachments`
- `GET /api/attachments/{attachmentId}/download`

求職者端：

- `GET /api/public/applications/{publicId}/offer?token=...`
- `POST /api/public/applications/{publicId}/offer/accept?token=...`
- `POST /api/public/applications/{publicId}/offer/decline?token=...`
- `GET /api/public/applications/{publicId}/resume?token=...`
- `POST /api/public/applications/{publicId}/resume?token=...`（multipart）

## 資料庫

Flyway `V10__job_offers_and_candidate_attachments.sql` 新增：

- `job_offers`
- `job_offer_histories`
- `candidate_attachments`

同時移除 `applicant_access_tokens.application_id` 的唯一限制，使同一筆 Application 可在不同通知中取得新的有效連結。每個 token 仍各自雜湊、具有效期且可撤銷。

## 設定

```env
RESUME_STORAGE_DIR=./data/resumes
RESUME_MAX_BYTES=5242880
RESUME_MAX_FILE_SIZE=5MB
RESUME_MAX_REQUEST_SIZE=6MB
```

Offer Email 實際寄送使用 `APP_MAIL_ENABLED` 與 Gmail SMTP 設定，詳見 [整合設定](INTEGRATIONS.md)。

## 驗收與開發環境

- 後端統一使用 Java 17；Maven 的 `release` 也固定為 17，避免 IDE 誤用 `source 8` 後讓所有 `record` 一起編譯失敗。
- `CandidateAttachmentIntegrationTests` 驗證有效 PDF、偽造 PDF、檔案大小、版本切換、跨部門權限及 Path Traversal 防護。
- `RecruitmentFlowIntegrationTests` 驗證從求職者應徵、面試、評分、錄取決策、Offer 審核寄出、PDF 履歷，到求職者接受 Offer 的完整流程。
- 目前完整測試結果：後端 43 項、前端 18 項，皆通過；前端 production build 亦通過。
- 本機 Gmail 密碼尚未換成 Google 應用程式密碼時，主 API 仍可啟動，但 Actuator 的 Mail health 會因 `BadCredentials` 顯示 `DOWN`。完成 Gmail 設定後再驗收真實寄信。

