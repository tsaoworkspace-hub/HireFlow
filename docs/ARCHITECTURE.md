# Architecture

## 系統邊界

系統以完整的招募主流程串接各功能：

```text
建立人力需求 → HR 核准 → 建立職缺 → 建立應徵紀錄
→ 安排面試 → 面試官評分 → 用人主管決策 → Audit Log
```

公司行事曆、Google Calendar、Email 通知及 Offer 回覆與招募主流程整合。

## Backend 分層

每個功能模組採 feature-first，再於模組內區分責任：

```text
feature/
├─ controller/   HTTP 與 DTO 邊界
├─ service/      Use case 與交易邊界
├─ repository/   資料存取
├─ entity/       持久化模型
└─ dto/          Request / Response
```

共用回應、錯誤處理、Security 與設定集中在 `common`，但不把商業規則塞進 common。

## Frontend 分層

```text
src/
├─ components/   可重用 UI
├─ layouts/      頁面骨架
├─ router/       路由與導航守衛
├─ services/     API Client
├─ stores/       跨頁狀態
└─ views/        路由頁面
```

## 重要決策

- 前後端透過 REST JSON 溝通。
- PostgreSQL Schema 由 Flyway 管理，禁止依賴 `ddl-auto=update`。
- 開發階段由 Vite Proxy 將 `/api` 轉送到 Spring Boot，避免前端寫死主機位址。
- 權限必須同時在後端驗證，前端隱藏按鈕不等於授權。

