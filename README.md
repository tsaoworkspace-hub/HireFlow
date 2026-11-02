# HireFlow

HireFlow 是一個招募、面試與簽核流程系統，也是用來練習業界開發流程的全端作品集專案。

## 技術架構

- Backend：Java 17、Spring Boot 4、Spring MVC、Spring Security、Spring Data JPA、Flyway
- Frontend：Vue 3、Vite、Vue Router、Pinia、Axios、Bootstrap 5
- Database：PostgreSQL
- Delivery：Maven Wrapper、npm、Docker Compose、GitHub Actions（後續加入）

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

下一步請由 `docs/DAY1_CHECKLIST.md` 開始。

