# HireFlow

HireFlow 是一個招募、面試與簽核流程系統，也是用來練習業界開發流程的全端作品集專案。

## 技術架構

- Backend：Java 17、Spring Boot 4、Spring MVC、Spring Security、Spring Data JPA、Flyway
- Frontend：Vue 3、Vite、Vue Router、Pinia、Axios、Bootstrap 5
- Database：PostgreSQL
- Delivery：Maven Wrapper、npm、Docker Compose、GitHub Actions

## 專案結構

```text
HireFlow/
├─ backend/       Spring Boot REST API
├─ frontend/      Vue SPA
├─ docs/          架構、API 規則與開發流程
├─ compose.yaml   本機 PostgreSQL
└─ README.md
```

## 第一次啟動

### 1. 啟動 PostgreSQL

```powershell
Copy-Item .env.example .env
docker compose up -d
```

### 2. 啟動後端

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

後端預設網址：`http://localhost:8080`  
健康檢查：`http://localhost:8080/api/health`

### 3. 啟動前端

```powershell
cd frontend
npm install
npm run dev
```

前端預設網址：`http://localhost:5173`

公開職缺入口：`http://localhost:5173/careers`

Swagger UI：`http://localhost:8080/swagger-ui.html`

## Demo 登入

啟動前後端後，開啟前端登入頁。四組帳號共用密碼：`HireFlow123!`

| 角色 | Email |
| --- | --- |
| ADMIN | `admin@hireflow.local` |
| HR | `hr@hireflow.local` |
| HIRING_MANAGER | `manager@hireflow.local` |
| INTERVIEWER | `interviewer@hireflow.local` |

登入採伺服器 Session，密碼以 BCrypt 雜湊保存。前端使用 CSRF Cookie 與 Header 保護狀態修改請求，受保護 API 會依登入狀態及角色回傳 401 或 403。

## 驗證指令

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm run test
npm run build
```

## 開發原則

1. 從 Issue／Ticket 與驗收條件開始，不直接開始寫畫面。
2. API、商業規則及資料庫異動必須有測試或可重現驗證方式。
3. 資料庫 Schema 只透過 Flyway Migration 變更。
4. 每個 Commit 只處理一個可描述的目的。
5. Secret 不進版控，使用環境變數與 `.env.example` 說明。

## 目前進度

- Day 1：Monorepo、Spring Boot、Vue、PostgreSQL、Flyway、Docker Compose 與 Dashboard 骨架。
- Day 2：Session 登入、BCrypt、四種 Demo 角色、401／403、CSRF、路由守衛與角色化導覽。
- Day 3：人力需求 CRUD、送出、HR 核准／退回／拒絕，以及核准後建立職缺。
- Day 4：應徵者、Application、查詢分頁、狀態歷程與招募看板。
- Day 5：面試安排、撞期檢查、面試官指派、評分送出鎖定與 HR 解鎖。
- Day 6：錄取決策、評分彙整、決策前置條件、全域 Audit Log、交易與核心整合測試。
- Day 7：GitHub Actions CI、OpenAPI／Swagger、公開 Demo 職缺與操作文件。
- Day 8：公開職缺頁、基本履歷、自傳、作品集與多筆工作經歷應徵表單。
- Day 9：一次性顯示的安全查詢連結、30 天效期與求職者應徵進度時間軸。
- Day 10：候選面試時段、求職者自助確認，以及依角色／部門隔離的公司行事曆。
- Day 11：Email Outbox、Gmail SMTP、應徵與面試通知，以及失敗重試。
- Day 12：Google Calendar OAuth、AES-GCM Token 加密與面試事件同步。
- Day 13：整合中心、Email／Calendar 工作紀錄、人工重試與交付文件。
- Day 14：Offer 草稿／主管核准／寄送／求職者回覆、到期提醒，以及 PDF 履歷附件。
- 下一步：保留在本機功能完成階段；尚未進入正式展示與部署整理。

環境與功能驗收項目記錄於 `docs/`，Day 10–13 設定方式見 `docs/DAY10-13.md`，Day 14 見 `docs/DAY14.md`。

